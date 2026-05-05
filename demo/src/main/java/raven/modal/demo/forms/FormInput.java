package raven.modal.demo.forms;

import static raven.modal.demo.utils.DemoPreferences.isSuggestionsEnabled;
import com.finals.db.DBConnection;
import com.finals.db.SimilarityUtil;
import com.finals.db.TimeLimit;

import net.miginfocom.swing.MigLayout;
import raven.modal.component.SimpleModalBorder;
import raven.modal.ModalDialog;
import raven.modal.Toast;
import raven.modal.listener.ModalCallback;
import raven.modal.listener.ModalController;
import raven.modal.demo.system.Form;
import raven.modal.demo.utils.DemoPreferences;
import raven.modal.demo.utils.SystemForm;
import raven.modal.toast.option.ToastLocation;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@SystemForm(name = "Add Research Title", description = "Input form for research titles")
public class FormInput extends Form {

    private JTextField titleField;
    private JTextField schoolYearField;
    private JComboBox<String> strandCombo;
    private JComboBox<SectionItem> sectionCombo;       // <-- now holds SectionItem objects
    private JComboBox<String> appliedCombo;
    private JComboBox<String> softwareCombo;
    private JComboBox<String> webpageCombo;
    private JPanel suggestionPanel;
    private Timer debounceTimer;

    // Inner class to store section id + name
    private static class SectionItem {
        int id;
        String name;

        SectionItem(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public FormInput() {
        init();
    }

    private void init() {
        setLayout(new MigLayout("wrap 2, insets 20 30 20 30, gap 10", "[right][fill,grow]"));
        setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY),
                "New Research Title", TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 14)));

        titleField = new JTextField();
        titleField.putClientProperty("JTextField.placeholderText", "Research Title");

        schoolYearField = new JTextField();
        schoolYearField.putClientProperty("JTextField.placeholderText", "YYYY-YYYY");

        strandCombo = new JComboBox<>(new String[]{"", "ICT", "GAS"});
        sectionCombo = new JComboBox<>();      // will be populated dynamically
        appliedCombo = new JComboBox<>(new String[]{"Yes", "Not yet", "No"});
        softwareCombo = new JComboBox<>(new String[]{"✔", "✘"});
        webpageCombo = new JComboBox<>(new String[]{"✔", "✘"});

        // Add fields to layout
        add(new JLabel("Research Title:"));
        add(titleField, "growx");
        add(new JLabel("School Year:"));
        add(schoolYearField, "growx");
        add(new JLabel("Strand:"));
        add(strandCombo, "growx");

        // Section dropdown (right after Strand)
        add(new JLabel("Section:"));
        add(sectionCombo, "growx");

        add(new JLabel("Applied:"));
        add(appliedCombo, "growx");
        add(new JLabel("Software:"));
        add(softwareCombo, "growx");
        add(new JLabel("Webpage:"));
        add(webpageCombo, "growx");

        // Suggestion panel for live similarity
        suggestionPanel = new JPanel();
        suggestionPanel.setLayout(new BoxLayout(suggestionPanel, BoxLayout.Y_AXIS));
        suggestionPanel.setBackground(new Color(245, 245, 245));
        suggestionPanel.setBorder(BorderFactory.createTitledBorder("Similar Titles Found"));
        suggestionPanel.setVisible(false);
        add(suggestionPanel, "span 2, growx");

        // Submit button
        JButton submitBtn = new JButton("Submit Research Title");
        submitBtn.setFont(submitBtn.getFont().deriveFont(Font.BOLD));
        add(submitBtn, "span 2, align center");

        // --- Strand selection reloads sections ---
        strandCombo.addActionListener(e -> loadSectionsForStrand((String) strandCombo.getSelectedItem()));

        // Debounce timer for similarity check
        debounceTimer = new Timer(300, e -> checkSimilarity());
        debounceTimer.setRepeats(false);

        titleField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { debounceTimer.restart(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { debounceTimer.restart(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { debounceTimer.restart(); }
        });

        submitBtn.addActionListener(this::submitForm);

        // Initially load sections for default strand (empty)
        loadSectionsForStrand("");
    }

    /**
     * Fetches sections from best_section_ict_c that contain the given strand
     * (e.g. "ICT" matches "ICT C", "ICT A"; "GAS" matches "GAS A" etc.).
     * If strand is empty, the combo is disabled and cleared.
     */
    private void loadSectionsForStrand(String strand) {
        sectionCombo.removeAllItems();
        if (strand == null || strand.isEmpty()) {
            sectionCombo.setEnabled(false);
            sectionCombo.addItem(new SectionItem(-1, "Select a strand first"));
            return;
        }

        // Load from MySQL only (where best_section_ict_c exists)
        try (Connection conn = DBConnection.getMySQLConnection()) {
            if (conn == null) {
                sectionCombo.setEnabled(false);
                sectionCombo.addItem(new SectionItem(-1, "DB unavailable"));
                return;
            }

            String sql = "SELECT id, name FROM `best_section_ict_c` WHERE name LIKE ? ORDER BY name";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, "%" + strand + "%");
                try (ResultSet rs = ps.executeQuery()) {
                    boolean hasItems = false;
                    while (rs.next()) {
                        int id = rs.getInt("id");
                        String name = rs.getString("name");
                        sectionCombo.addItem(new SectionItem(id, name));
                        hasItems = true;
                    }
                    if (!hasItems) {
                        sectionCombo.addItem(new SectionItem(-1, "No sections found"));
                    }
                }
            }
            sectionCombo.setEnabled(true);
        } catch (SQLException ex) {
            ex.printStackTrace();
            sectionCombo.setEnabled(false);
            sectionCombo.addItem(new SectionItem(-1, "Error loading sections"));
        }
    }

    private void checkSimilarity() {
        if (!DemoPreferences.isSuggestionsEnabled()) {
            suggestionPanel.setVisible(false);
            return;
        }
        String title = titleField.getText().trim();
        if (title.length() < 5) {
            suggestionPanel.removeAll();
            suggestionPanel.setVisible(false);
            return;
        }
        List<TimeLimit> matches = SimilarityUtil.getDetailedSimilarTitles(title);
        suggestionPanel.removeAll();
        if (matches.isEmpty()) {
            suggestionPanel.add(new JLabel("No similar titles found."));
        } else {
            for (int i = 0; i < Math.min(matches.size(), 3); i++) {
                TimeLimit m = matches.get(i);
                JLabel label = new JLabel(String.format("%s (%.0f%% match)", m.title, m.score * 100));
                suggestionPanel.add(label);
            }
        }
        suggestionPanel.setVisible(true);
        suggestionPanel.revalidate();
        suggestionPanel.repaint();
    }

    private void submitForm(ActionEvent e) {
        String title = titleField.getText().trim();
        String sy = schoolYearField.getText().trim();
        String strand = (String) strandCombo.getSelectedItem();

        // Validate
        if (title.length() < 10 || title.length() > 255) {
            showError("Title must be between 10 and 255 characters.");
            return;
        }
        if (!sy.matches("\\d{4}-\\d{4}")) {
            showError("School Year must be in format YYYY-YYYY.");
            return;
        }
        if (strand.isEmpty()) {
            showError("Please select a strand.");
            return;
        }

        // Validate section
        SectionItem selectedSection = (SectionItem) sectionCombo.getSelectedItem();
        if (selectedSection == null || selectedSection.id == -1) {
            showError("Please select a valid section.");
            return;
        }

        // Check similarity threshold
        List<TimeLimit> matches = SimilarityUtil.getDetailedSimilarTitles(title);
        if (!matches.isEmpty() && matches.get(0).score > 0.75) {
            boolean proceed = showConfirm(
                    "This title is very similar to:\n" + matches.get(0).title + "\n\nProceed anyway?");
            if (!proceed) return;
        }

        // Insert into DB – use MySQL for consistency with sections
        Connection conn = DBConnection.getMySQLConnection();
        if (conn == null) {
            showError("Database connection unavailable. Please try again later.");
            return;
        }

        String sql = "INSERT INTO aclc_research_titles (`Research Title`, `SY-YR`, `Status`, `Approved by`, " +
                     "`Applied`, `Strand`, `Software`, `Webpage`, `section_id`, record_state, last_updated) " +
                     "VALUES (?, ?, 'Pending', NULL, ?, ?, ?, ?, ?, 'ACTIVE', NOW())";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, sy);
            ps.setString(3, (String) appliedCombo.getSelectedItem());
            ps.setString(4, strand);
            ps.setString(5, (String) softwareCombo.getSelectedItem());
            ps.setString(6, (String) webpageCombo.getSelectedItem());
            ps.setInt(7, selectedSection.id);
            ps.executeUpdate();

            Toast.show(SwingUtilities.getWindowAncestor(this), Toast.Type.SUCCESS,
                    "Research title added successfully!", ToastLocation.TOP_TRAILING);

            // Clear fields
            titleField.setText("");
            schoolYearField.setText("");
            strandCombo.setSelectedIndex(0);
            appliedCombo.setSelectedIndex(0);
            softwareCombo.setSelectedIndex(0);
            webpageCombo.setSelectedIndex(0);
            suggestionPanel.removeAll();
            suggestionPanel.setVisible(false);
        } catch (SQLException ex) {
            showError("Database error: " + ex.getMessage());
        }
    }

    private void showError(String msg) {
        SimpleModalBorder modal = new SimpleModalBorder(
                new JLabel("<html><body style='width:250px'>" + msg + "</body></html>"),
                "Error",
                SimpleModalBorder.DEFAULT_OPTION,
                (controller, action) -> controller.close()
        );
        ModalDialog.showModal(SwingUtilities.getWindowAncestor(this), modal, ModalDialog.createOption());
    }

    private boolean showConfirm(String msg) {
        final boolean[] result = {false};
        SimpleModalBorder modal = new SimpleModalBorder(
                new JLabel("<html><body style='width:250px'>" + msg + "</body></html>"),
                "Confirm",
                SimpleModalBorder.YES_NO_OPTION,
                new ModalCallback() {
                    @Override
                    public void action(ModalController controller, int action) {
                        result[0] = (action == SimpleModalBorder.YES_OPTION);
                        controller.close();
                    }
                }
        );
        ModalDialog.showModal(SwingUtilities.getWindowAncestor(this), modal, ModalDialog.createOption());
        return result[0];
    }
}