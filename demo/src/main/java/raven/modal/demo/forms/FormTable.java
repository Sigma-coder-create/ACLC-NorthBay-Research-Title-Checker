package raven.modal.demo.forms;

import com.finals.db.DBConnection;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import net.miginfocom.swing.MigLayout;
import raven.modal.ModalDialog;
import raven.modal.component.SimpleModalBorder;
import raven.modal.demo.system.Form;
import raven.modal.demo.utils.SystemForm;
import raven.modal.demo.utils.table.TableHeaderAlignment;
import raven.modal.demo.utils.table.CheckBoxTableHeaderRenderer;
import raven.modal.option.Location;
import raven.modal.option.Option;
import raven.swingpack.JPagination;
import raven.modal.Toast;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import com.finals.db.SimilarityUtil;
import com.finals.db.TimeLimit;
import raven.modal.demo.component.pagination.PaginationAnimation;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.sql.*;
import java.text.DecimalFormat;
import java.util.Vector;
import java.util.List;
import java.util.ArrayList;

@SystemForm(name = "Table", description = "table is a user interface component", tags = {"list"})
public class FormTable extends Form {

    public FormTable() {
        init();
    }

    private void init() {
        setLayout(new MigLayout("fillx,wrap", "[fill]", "[][fill,grow]"));
        add(createInfo("Custom Table", "A table is a user interface component that displays a collection of records in a structured, tabular format. It allows users to view, sort, and manage data or other resources.", 1));
        add(createTab(), "gapx 7 7");
    }

    private void searchTable() {
        currentSearch = txtBasicSearch.getText().trim();
        pagination.setSelectedPage(1);
        showData(1, currentSearch);
    }

    @Override
    public void formInit() {
        Object[] columns = new Object[]{"#", "Research Title", "SY-YR", "Status",
            "Approved by", "Applied", "Strand", "Software", "Webpage", "Research Paper"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        basicTable.setModel(model);

        // ---------- column widths (after setModel) ----------
        basicTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

        basicTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        basicTable.getColumnModel().getColumn(0).setMaxWidth(50);

        basicTable.getColumnModel().getColumn(1).setPreferredWidth(300);    // no max, can grow

        basicTable.getColumnModel().getColumn(2).setPreferredWidth(80);
        basicTable.getColumnModel().getColumn(2).setMaxWidth(120);

        basicTable.getColumnModel().getColumn(3).setPreferredWidth(90);
        basicTable.getColumnModel().getColumn(3).setMaxWidth(120);

        basicTable.getColumnModel().getColumn(4).setPreferredWidth(170);
        basicTable.getColumnModel().getColumn(4).setMaxWidth(400);

        basicTable.getColumnModel().getColumn(5).setPreferredWidth(80);
        basicTable.getColumnModel().getColumn(5).setMaxWidth(120);

        basicTable.getColumnModel().getColumn(6).setPreferredWidth(60);
        basicTable.getColumnModel().getColumn(6).setMaxWidth(100);

        basicTable.getColumnModel().getColumn(7).setPreferredWidth(70);
        basicTable.getColumnModel().getColumn(7).setMaxWidth(100);

        basicTable.getColumnModel().getColumn(8).setPreferredWidth(70);
        basicTable.getColumnModel().getColumn(8).setMaxWidth(100);

        basicTable.getColumnModel().getColumn(9).setPreferredWidth(70);
        basicTable.getColumnModel().getColumn(9).setMaxWidth(100);

        formRefresh();
    }

    private void showData(int page) {
        showData(page, currentSearch);
    }

    private void showData(int page, String search) {
        DefaultTableModel model = (DefaultTableModel) basicTable.getModel();
        model.setRowCount(0);
        int offset = (page - 1) * limit;

        try (Connection conn = DBConnection.getMySQLConnection()) {
            if (conn == null) {
                System.err.println("Database connection is null! Check Tailscale/Internet.");
                return;
            }

            // Exclude 'Hard Bind' as well as 'DELETED'
            StringBuilder whereClause = new StringBuilder("WHERE record_state = 'ACTIVE' AND Status != 'Hard Bind'");
            if (!search.isEmpty()) {
                whereClause.append(" AND (`Research Title` LIKE ? OR `SY-YR` LIKE ? OR Strand LIKE ? OR "
                        + "`Approved by` LIKE ? OR Applied LIKE ? OR Software LIKE ? OR Webpage LIKE ?"
                        + " OR `Research Paper` LIKE ?)");   // added Research Paper to search
            }
            if (selectedSectionId != -1) {
                whereClause.append(" AND section_id = ?");
            }

            String countSql = "SELECT COUNT(*) FROM aclc_research_titles " + whereClause;
            try (PreparedStatement pstmt = conn.prepareStatement(countSql)) {
                int paramIndex = 1;
                if (!search.isEmpty()) {
                    String like = "%" + search + "%";
                    for (int i = 0; i < 8; i++) {   // now 8 searchable columns
                        pstmt.setString(i + 1, like);
                    }
                    paramIndex = 9;
                }
                if (selectedSectionId != -1) {
                    pstmt.setInt(paramIndex, selectedSectionId);
                }
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        int total = rs.getInt(1);
                        lbTotalPage.setText(DecimalFormat.getInstance().format(total));
                        int totalPages = (int) Math.ceil((double) total / limit);
                        pagination.getModel().setPageRange(page, Math.max(1, totalPages));
                    }
                }
            }

            // data query – added Research Paper
            String dataSql = "SELECT ID, `Research Title`, `SY-YR`, Status, `Approved by`, Applied, Strand, Software, Webpage, `Research Paper` "
                    + "FROM aclc_research_titles " + whereClause + " LIMIT ? OFFSET ?";
            try (PreparedStatement pstmt = conn.prepareStatement(dataSql)) {
                int paramIndex = 1;
                if (!search.isEmpty()) {
                    String like = "%" + search + "%";
                    for (int i = 0; i < 8; i++) {
                        pstmt.setString(paramIndex++, like);
                    }
                }
                if (selectedSectionId != -1) {
                    pstmt.setInt(paramIndex++, selectedSectionId);
                }
                pstmt.setInt(paramIndex++, limit);
                pstmt.setInt(paramIndex, offset);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        model.addRow(new Object[]{
                            rs.getInt("ID"),
                            rs.getString("Research Title"),
                            rs.getString("SY-YR"),
                            rs.getString("Status"),
                            rs.getString("Approved by"),
                            rs.getString("Applied"),
                            rs.getString("Strand"),
                            rs.getString("Software"),
                            rs.getString("Webpage"),
                            rs.getString("Research Paper")
                        });
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("SQL Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadSections(String search) {
        JTable table = ((JTable) ((JScrollPane) customTablePanel.getComponent(2)).getViewport().getView());
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);

        try (Connection conn = DBConnection.getMySQLConnection()) {
            if (conn == null) return;

            String where = "1=1";
            if (search != null && !search.isEmpty()) {
                where = "s.name LIKE ?";
            }

            String sql = "SELECT s.id, s.name, s.created_at, COUNT(t.ID) as cnt "
                       + "FROM `best_section_ict_c` s "
                       + "LEFT JOIN `aclc_research_titles` t ON s.id = t.section_id AND t.record_state = 'ACTIVE' "
                       + "WHERE " + where + " "
                       + "GROUP BY s.id, s.name, s.created_at "
                       + "ORDER BY s.created_at DESC";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                if (!search.isEmpty()) {
                    ps.setString(1, "%" + search + "%");
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        model.addRow(new Object[]{
                            false,                  // checkbox
                            rs.getInt("cnt"),       // #
                            rs.getString("name"),   // Section Name
                            rs.getTimestamp("created_at"), // Date
                            "View",                 // Button text
                            rs.getInt("id")         // hidden section id
                        });
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showToast(String message, String type) {
        Toast.Type toastType;
        switch (type) {
            case "success": toastType = Toast.Type.SUCCESS; break;
            case "error":   toastType = Toast.Type.ERROR;   break;
            case "warning": toastType = Toast.Type.WARNING; break;
            case "info":    toastType = Toast.Type.INFO;    break;
            default:        toastType = Toast.Type.DEFAULT;
        }
        Toast.show(this, toastType, message);
    }

    private JPanel createInfo(String title, String description, int level) {
        JPanel panel = new JPanel(new MigLayout("fillx,wrap", "[fill]"));
        JLabel lbTitle = new JLabel(title);
        JTextPane text = new JTextPane();
        text.setText(description);
        text.setEditable(false);
        text.setBorder(BorderFactory.createEmptyBorder());
        lbTitle.putClientProperty(FlatClientProperties.STYLE, "" +
                "font:bold +" + (4 - level));
        panel.add(lbTitle);
        panel.add(text, "width 500");
        return panel;
    }

    @Override
    public void formRefresh() {
        if (pagination != null) {
            showData(pagination.getSelectedPage(), currentSearch);
        }
    }

    private JPanel createBasicHeaderAction() {
        JPanel panel = new JPanel(new MigLayout("insets 2 10 2 10", "[fill,230]push[][]"));

        txtBasicSearch = new JTextField();
        txtBasicSearch.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Search...");
        txtBasicSearch.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON,
                new FlatSVGIcon("raven/modal/demo/icons/search.svg", 0.4f));

        // Live database search
        txtBasicSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { searchTable(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { searchTable(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { searchTable(); }
        });

        // Autocomplete suggestion popup
        JPopupMenu suggestionMenu = new JPopupMenu();
        txtBasicSearch.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                String query = txtBasicSearch.getText().trim();
                suggestionMenu.removeAll();
                if (query.length() >= 2) {
                    for (int i = 0; i < basicTable.getRowCount(); i++) {
                        Object value = basicTable.getValueAt(i, 1);
                        if (value != null) {
                            String title = value.toString();
                            if (title.toLowerCase().contains(query.toLowerCase())) {
                                JMenuItem item = new JMenuItem(title);
                                item.addActionListener(ae -> {
                                    txtBasicSearch.setText(title);
                                    suggestionMenu.setVisible(false);
                                });
                                suggestionMenu.add(item);
                            }
                        }
                        if (suggestionMenu.getComponentCount() >= 5) break;
                    }
                    if (suggestionMenu.getComponentCount() > 0) {
                        suggestionMenu.show(txtBasicSearch, 0, txtBasicSearch.getHeight());
                        txtBasicSearch.requestFocus();
                    } else {
                        suggestionMenu.setVisible(false);
                    }
                } else {
                    suggestionMenu.setVisible(false);
                }
            }
        });

        JButton btnShowAll = new JButton("Show All");
        btnShowAll.addActionListener(e -> {
            selectedSectionId = -1;
            lbSectionFilter.setText(" (All Sections)");
            searchTable();
        });

        JButton btnEdit = new JButton("Edit");
        JButton btnDelete = new JButton("Delete");
        btnEdit.addActionListener(e -> editSelected());
        btnDelete.addActionListener(e -> deleteSelected());
        JButton btnTest = new JButton("Test");
        btnTest.addActionListener(e -> {
            int row = basicTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this,
                        "Please select a research title first.",
                        "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int selectedId = (int) basicTable.getValueAt(row, 0);           // column 0 = ID
            String selectedTitle = (String) basicTable.getValueAt(row, 1);  // column 1 = Title

            if (selectedTitle == null || selectedTitle.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Selected title is empty.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            final int currentId = selectedId;

            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                private final java.util.List<TimeLimit> results = new java.util.ArrayList<>();

                @Override
                protected Void doInBackground() throws Exception {
                    java.util.List<TimeLimit> raw = SimilarityUtil.getDetailedSimilarTitles(selectedTitle);
                    for (TimeLimit t : raw) {
                        if (t.score >= 0.7 && t.id != currentId) {
                            results.add(t);
                        }
                    }
                    return null;
                }

                @Override
                protected void done() {
                    if (results.isEmpty()) {
                        JOptionPane.showMessageDialog(FormTable.this,
                                "No very similar titles found (70%-99% match).",
                                "Test Result", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        showSingleTitleTestDialog(selectedTitle, results);
                    }
                }
            };
            worker.execute();
        });
        JButton btnScanAll = new JButton("Scan All");
            btnScanAll.addActionListener(e -> {
                int choice = JOptionPane.showConfirmDialog(this,
                        "Run a full duplicate scan on ALL titles? This may take a moment.",
                        "Full Duplicate Scan", JOptionPane.YES_NO_OPTION);
                if (choice == JOptionPane.YES_OPTION) {
                    runDuplicateTest();   // method must exist – see next step
                }
            }); 
        
        panel.add(txtBasicSearch, "growx");
        panel.add(btnShowAll);
        panel.add(btnEdit);
        panel.add(btnDelete);
        panel.add(btnTest); 
        panel.add(btnScanAll);

        panel.putClientProperty(FlatClientProperties.STYLE, "background:null;");
        return panel;
    }
    
    private void runDuplicateTest() {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            private final java.util.List<DuplicateResult> duplicates = new java.util.ArrayList<>();

            @Override
            protected Void doInBackground() throws Exception {
                SimilarityUtil.initializeFromDatabases();

                java.util.List<TitleRecord> titles = new java.util.ArrayList<>();
                try (Connection conn = DBConnection.getMySQLConnection()) {
                    if (conn == null) return null;
                    String sql = "SELECT `Research Title`, `SY-YR`, Status FROM aclc_research_titles WHERE record_state = 'ACTIVE'";
                    try (Statement stmt = conn.createStatement();
                        ResultSet rs = stmt.executeQuery(sql)) {
                        while (rs.next()) {
                            titles.add(new TitleRecord(
                                    rs.getString("Research Title"),
                                    rs.getString("SY-YR"),
                                    rs.getString("Status")));
                        }
                    }
                } catch (SQLException ex) { ex.printStackTrace(); return null; }

                titles.removeIf(t -> "Hard Bind".equalsIgnoreCase(t.status));

                for (int i = 0; i < titles.size(); i++) {
                    for (int j = i + 1; j < titles.size(); j++) {
                        TitleRecord a = titles.get(i);
                        TitleRecord b = titles.get(j);
                        double score = SimilarityUtil.calculateSimilarity(a.title, b.title);
                        if (score >= 0.7) {   // includes 100% matches
                            duplicates.add(new DuplicateResult(a.title, a.syYr, b.title, b.syYr, score));
                        }
                    }
                }
                return null;
            }

            @Override
            protected void done() {
                if (duplicates.isEmpty()) {
                    JOptionPane.showMessageDialog(FormTable.this,
                            "No duplicates found (threshold 70%).", "Test Result", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    showDuplicateDialog(duplicates);
                }
            }
        };
        worker.execute();
    }

    // Helper classes (should be placed inside FormTable class, not inside a method)
    private static class TitleRecord {
        String title;
        String syYr;
        String status;          // <-- add this

        TitleRecord(String t, String s, String st) {
            title = t;
            syYr = s;
            status = st;
        }
    }

    private static class DuplicateResult {
        String title1, syYr1, title2, syYr2;
        double similarity;
        DuplicateResult(String t1, String s1, String t2, String s2, double sim) {
            title1 = t1; syYr1 = s1;
            title2 = t2; syYr2 = s2;
            similarity = sim;
        }
    }
    private void showSingleTitleTestDialog(String queryTitle, java.util.List<TimeLimit> matches) {
    JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                "Similarity Check for: " + truncate(queryTitle, 60), true);
        dialog.setLayout(new BorderLayout());

        JLabel header = new JLabel("Matches for: " + queryTitle);
        header.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        dialog.add(header, BorderLayout.NORTH);

        String[] columnNames = {"Matched Title", "Year", "Similarity"};
        DefaultTableModel model = new DefaultTableModel(columnNames, 0);
        for (TimeLimit m : matches) {
            model.addRow(new Object[]{
                    m.title,
                    m.dateStr,    // or m.dateStr if the field is named differently
                    String.format("%.0f%%", m.score * 100)
            });
        }

        JTable table = new JTable(model);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.getColumnModel().getColumn(0).setPreferredWidth(450);
        table.getColumnModel().getColumn(1).setPreferredWidth(80);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(700, 300));
        dialog.add(scrollPane, BorderLayout.CENTER);

        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dialog.dispose());
        JPanel bottom = new JPanel();
        bottom.add(closeBtn);
        dialog.add(bottom, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private String truncate(String s, int len) {
        if (s == null) return "";
        return s.length() <= len ? s : s.substring(0, len - 3) + "...";
    }
    private void showDuplicateDialog(java.util.List<DuplicateResult> duplicates) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Duplicate Detection Results", true);
        dialog.setLayout(new BorderLayout());

        String[] columnNames = {"Title 1", "Year 1", "Title 2", "Year 2", "Similarity"};
        DefaultTableModel model = new DefaultTableModel(columnNames, 0);
        for (DuplicateResult d : duplicates) {
            model.addRow(new Object[]{
                    d.title1, d.syYr1,
                    d.title2, d.syYr2,
                    String.format("%.0f%%", d.similarity * 100)
            });
        }

        JTable table = new JTable(model);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.getColumnModel().getColumn(0).setPreferredWidth(300);
        table.getColumnModel().getColumn(2).setPreferredWidth(300);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(900, 400));
        dialog.add(scrollPane, BorderLayout.CENTER);

        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dialog.dispose());
        JPanel bottom = new JPanel();
        bottom.add(closeBtn);
        dialog.add(bottom, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
    private void editSelected() {
    int row = basicTable.getSelectedRow();
    if (row == -1) {
        showToast("Please select a row to edit.", "warning");
        return;
    }

    DefaultTableModel model = (DefaultTableModel) basicTable.getModel();
    int id = (int) model.getValueAt(row, 0);
    String title = (String) model.getValueAt(row, 1);
    String syyr = (String) model.getValueAt(row, 2);
    String status = (String) model.getValueAt(row, 3);
    String approved = (String) model.getValueAt(row, 4);
    String applied = (String) model.getValueAt(row, 5);
    String strand = (String) model.getValueAt(row, 6);
    String software = (String) model.getValueAt(row, 7);
    String webpage = (String) model.getValueAt(row, 8);
    String researchPaper = (String) model.getValueAt(row, 9);

    // Retrieve current section_id
    int currentSectionId = -1;
    try (Connection conn = DBConnection.getMySQLConnection()) {
        if (conn != null) {
            PreparedStatement ps = conn.prepareStatement("SELECT section_id FROM aclc_research_titles WHERE ID = ?");
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                currentSectionId = rs.getInt("section_id");
            }
        }
    } catch (SQLException ex) {
        ex.printStackTrace();
    }

    EditResearchPanel editPanel = new EditResearchPanel(title, syyr, status, approved, applied, strand, software, webpage, researchPaper, currentSectionId);
    Option option = ModalDialog.createOption();
    option.getLayoutOption().setSize(-1, 1f).setLocation(Location.CENTER, Location.CENTER);

    ModalDialog.showModal(this, new SimpleModalBorder(editPanel, "Edit Research Title",
            SimpleModalBorder.YES_NO_OPTION,
            (controller, action) -> {
                if (action == SimpleModalBorder.YES_OPTION) {
                    try (Connection conn = DBConnection.getMySQLConnection()) {
                        if (conn == null) return;
                        String sql = "UPDATE aclc_research_titles SET `Research Title`=?, `SY-YR`=?, Status=?, `Approved by`=?, Applied=?, Strand=?, Software=?, Webpage=?, `Research Paper`=?, section_id=? WHERE ID=?";
                        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                            pstmt.setString(1, editPanel.getTitleText());
                            pstmt.setString(2, editPanel.getSyYrText());
                            pstmt.setString(3, editPanel.getStatusText());
                            pstmt.setString(4, editPanel.getApprovedByText());
                            pstmt.setString(5, editPanel.getAppliedText());
                            pstmt.setString(6, editPanel.getStrandText());
                            pstmt.setString(7, editPanel.getSoftwareText());
                            pstmt.setString(8, editPanel.getWebpageText());
                            pstmt.setString(9, editPanel.getResearchPaperText());
                            pstmt.setInt(10, editPanel.getSectionId());
                            pstmt.setInt(11, id);
                            pstmt.executeUpdate();
                        }
                        showToast("Record updated successfully.", "success");
                        formRefresh();
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                        showToast("Update failed: " + ex.getMessage(), "error");
                    }
                }
            }), option);
    }

    private void deleteSelected() {
        int row = basicTable.getSelectedRow();
        if (row == -1) {
            showToast("Please select a row to delete.", "warning");
            return;
        }
        int id = (int) basicTable.getValueAt(row, 0);
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this record?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = DBConnection.getMySQLConnection()) {
                if (conn == null) return;
                String sql = "UPDATE aclc_research_titles SET record_state='DELETED' WHERE ID=?";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setInt(1, id);
                    pstmt.executeUpdate();
                }
                showToast("Record deleted successfully.", "success");
                formRefresh();
            } catch (SQLException ex) {
                ex.printStackTrace();
                showToast("Delete failed: " + ex.getMessage(), "error");
            }
        }
    }

    private class EditResearchPanel extends JPanel {
        private JTextField txtTitle, txtSyYr, txtApproved;
        private JComboBox<String> strandCombo, appliedCombo, softwareCombo, webpageCombo, statusCombo, researchPaperCombo;
        private JComboBox<SectionItem> sectionCombo;

        // Simple inner class for section id+name
        private class SectionItem {
            int id;
            String name;
            SectionItem(int id, String name) { this.id = id; this.name = name; }
            public String toString() { return name; }
        }

        public EditResearchPanel(String title, String syyr, String status, String approved,
                                String applied, String strand, String software, String webpage, String researchPaper, int currentSectionId) {
            setLayout(new MigLayout("wrap 2, insets 10", "[right][fill,grow]", "[]10[]"));

            // Title
            add(new JLabel("Research Title:"));
            txtTitle = new JTextField(title);
            add(txtTitle, "growx");

            // SY-YR
            add(new JLabel("SY-YR:"));
            txtSyYr = new JTextField(syyr);
            add(txtSyYr, "growx");

            // Status
            add(new JLabel("Status:"));
        String[] statuses = {"Pending", "Approved", "Denied", "Hard Bind"};
        statusCombo = new JComboBox<>(statuses);      // correct: JComboBox<String> inferred from left side
        statusCombo.setSelectedItem(status);
        add(statusCombo, "growx");

            // Approved by
            add(new JLabel("Approved by:"));
            txtApproved = new JTextField(approved);
            add(txtApproved, "growx");

            // Applied (combo)
            add(new JLabel("Applied:"));
            String[] appliedOptions = {"Yes", "Not yet", "No"};
            appliedCombo = new JComboBox<>(appliedOptions);
            appliedCombo.setSelectedItem(applied);
            add(appliedCombo, "growx");

            // Strand (combo) – triggers section reload
            add(new JLabel("Strand:"));
            String[] strands = {"", "ICT", "GAS"};
            strandCombo = new JComboBox<>(strands);
            strandCombo.setSelectedItem(strand);
            add(strandCombo, "growx");

            // Section (combo) – initially loaded based on current strand
            add(new JLabel("Section:"));
            sectionCombo = new JComboBox<>();
            add(sectionCombo, "growx");

            // Software (combo)
            add(new JLabel("Software:"));
            String[] yesNo = {"✔", "✘"};
            softwareCombo = new JComboBox<>(yesNo);
            softwareCombo.setSelectedItem(software);
            add(softwareCombo, "growx");

            // Webpage (combo)
            add(new JLabel("Webpage:"));
            webpageCombo = new JComboBox<>(yesNo);
            webpageCombo.setSelectedItem(webpage);
            add(webpageCombo, "growx");

            add(new JLabel("Research Paper:"));
            researchPaperCombo = new JComboBox<>(yesNo);
            researchPaperCombo.setSelectedItem(researchPaper);
            add(researchPaperCombo, "growx");

            // Load sections for the initial strand, and select the current section if possible
            loadSectionsForStrand(strand);
            for (int i = 0; i < sectionCombo.getItemCount(); i++) {
                SectionItem item = sectionCombo.getItemAt(i);
                if (item != null && item.id == currentSectionId) {
                    sectionCombo.setSelectedItem(item);
                    break;
                }
            }

            strandCombo.addActionListener(e -> loadSectionsForStrand((String) strandCombo.getSelectedItem()));
        }

        private void loadSectionsForStrand(String strand) {
            sectionCombo.removeAllItems();
            if (strand == null || strand.isEmpty()) {
                sectionCombo.setEnabled(false);
                return;
            }
            try (Connection conn = DBConnection.getMySQLConnection()) {
                if (conn == null) {
                    sectionCombo.setEnabled(false);
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
            }
        }

        // Getters
        public String getTitleText() { return txtTitle.getText(); }
        public String getSyYrText() { return txtSyYr.getText(); }
        public String getStatusText() { return (String) statusCombo.getSelectedItem(); }
        public String getApprovedByText() { return txtApproved.getText(); }
        public String getAppliedText() { return (String) appliedCombo.getSelectedItem(); }
        public String getStrandText() { return (String) strandCombo.getSelectedItem(); }
        public String getSoftwareText() { return (String) softwareCombo.getSelectedItem(); }
        public String getWebpageText() { return (String) webpageCombo.getSelectedItem(); }
        public String getResearchPaperText() { return (String) researchPaperCombo.getSelectedItem(); }
        public int getSectionId() {
            SectionItem item = (SectionItem) sectionCombo.getSelectedItem();
            return item != null ? item.id : -1;
        }
    }


    private Component createTab() {
        JTabbedPane tabb = new JTabbedPane();
        tabb.putClientProperty(FlatClientProperties.STYLE, "tabType:card");
        tabb.addTab("Basic table", createBorder(createBasicTable()));
        tabb.addTab("Custom table", createBorder(createCustomTable()));
        tabb.addTab("Hard Bound", createBorder(createHardBindTable()));   // <-- new
        return tabb;
    }

    private Component createBorder(Component component) {
        JPanel panel = new JPanel(new MigLayout("fill,insets 7 0 7 0", "[fill]", "[fill]"));
        panel.add(component);
        return panel;
    }

    private Component createCustomTable() {
        JPanel panel = new JPanel(new MigLayout("fillx,wrap,insets 15 0 10 0", "[fill]", "[][][fill,grow]"));
        customTablePanel = panel;

        // Columns: [SELECT] | # | Section Name | Date Created | View | id (hidden)
        Object[] columns = new Object[]{"SELECT", "#", "Section Name", "Date Created", "View", "id"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0 || column == 4; // checkbox and View button
            }
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) return Boolean.class;
                if (columnIndex == 1) return Integer.class;
                if (columnIndex == 3) return Timestamp.class;
                return String.class;
            }
        };

        JTable table = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        // Hide the id column (index 5)
        table.getColumnModel().getColumn(5).setMinWidth(0);
        table.getColumnModel().getColumn(5).setMaxWidth(0);
        table.getColumnModel().getColumn(5).setWidth(0);

        table.getColumnModel().getColumn(0).setMaxWidth(50);
        table.getColumnModel().getColumn(1).setMaxWidth(50);
        table.getColumnModel().getColumn(3).setPreferredWidth(120);
        table.getColumnModel().getColumn(4).setMaxWidth(80); // View button column

        // Set button renderer & editor on the View column (index 4)
        table.getColumnModel().getColumn(4).setCellRenderer(new ButtonRenderer());
        table.getColumnModel().getColumn(4).setCellEditor(new ButtonEditor(new JCheckBox()));

        table.getTableHeader().setReorderingAllowed(false);

        // Checkbox header (as requested)
        table.getColumnModel().getColumn(0).setHeaderRenderer(new CheckBoxTableHeaderRenderer(table, 0));

        // Alignment
        table.getTableHeader().setDefaultRenderer(new TableHeaderAlignment(table) {
            @Override
            protected int getAlignment(int column) {
                if (column == 1) {
                    return SwingConstants.CENTER;
                }
                return SwingConstants.LEADING;
            }
        });

        // Styling
        panel.putClientProperty(FlatClientProperties.STYLE, "" +
                "arc:10;" +
                "background:$Table.background;");
        table.getTableHeader().putClientProperty(FlatClientProperties.STYLE, "" +
                "height:30;" +
                "hoverBackground:null;" +
                "pressedBackground:null;" +
                "separatorColor:$TableHeader.background;");
        table.putClientProperty(FlatClientProperties.STYLE, "" +
                "rowHeight:30;" +
                "showHorizontalLines:true;" +
                "intercellSpacing:0,1;" +
                "cellFocusColor:$TableHeader.hoverBackground;" +
                "selectionBackground:$TableHeader.hoverBackground;" +
                "selectionInactiveBackground:$TableHeader.hoverBackground;" +
                "selectionForeground:$Table.foreground;");
        scrollPane.getVerticalScrollBar().putClientProperty(FlatClientProperties.STYLE, "" +
                "trackArc:$ScrollBar.thumbArc;" +
                "trackInsets:3,3,3,3;" +
                "thumbInsets:3,3,3,3;" +
                "background:$Table.background;");

        JLabel title = new JLabel("Sections");
        title.putClientProperty(FlatClientProperties.STYLE, "font:bold +2");
        panel.add(title, "gapx 20");

        panel.add(createHeaderAction(), "growx");
        panel.add(scrollPane, "grow, push");

        // Load sections from DB
        loadSections("");

        return panel;
    }

    private Component createBasicTable() {
        JPanel panelTable = new JPanel(new MigLayout("fillx,wrap,insets 15 0 10 0", "[fill]", "[][][fill,grow][]"));

        Object[] columns = new Object[]{"#", "Research Title", "SY-YR", "Status", "Approved by", "Applied", "Strand", "Software", "Webpage", "Research Paper"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        JTable table = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        // --- title and section filter on the same line to save space ---
        JLabel title = new JLabel("Research Titles");
        title.putClientProperty(FlatClientProperties.STYLE, "font:bold +2");

        lbSectionFilter = new JLabel(" (All Sections)");
        lbSectionFilter.putClientProperty(FlatClientProperties.STYLE, "foreground:$Text.tertiary;");

        JPanel titlePanel = new JPanel(new MigLayout("insets 0", "[][][fill]", ""));
        titlePanel.setOpaque(false);
        titlePanel.add(title);
        titlePanel.add(lbSectionFilter);
        panelTable.add(titlePanel, "gapbottom 5, wrap");   // small gap after title line
        // ------------------------------------------------

        panelTable.add(createBasicHeaderAction(), "growx");
        panelTable.add(scrollPane, "grow, push");

        // pagination
        pagination = new JPagination(11, 1, 1);
        pagination.addChangeListener(e -> showData(pagination.getSelectedPage()));
        JPanel panelPage = new JPanel(new MigLayout("insets 5 15 5 15", "[][]push[]"));
        lbTotalPage = new JLabel("0");
        pagination.putClientProperty(FlatClientProperties.STYLE, "background:null;");
        panelPage.putClientProperty(FlatClientProperties.STYLE, "background:null;");
        panelPage.add(new JLabel("Total:"));
        panelPage.add(lbTotalPage);
        panelPage.add(pagination);
        panelTable.add(panelPage);

        // style (unchanged)
        panelTable.putClientProperty(FlatClientProperties.STYLE, "" +
                "arc:10;" +
                "background:$Table.background;");
        table.getTableHeader().putClientProperty(FlatClientProperties.STYLE, "" +
                "height:30;" +
                "hoverBackground:null;" +
                "pressedBackground:null;" +
                "separatorColor:$TableHeader.background;");
        table.putClientProperty(FlatClientProperties.STYLE, "" +
                "rowHeight:30;" +
                "showHorizontalLines:true;" +
                "intercellSpacing:0,1;" +
                "cellFocusColor:$TableHeader.hoverBackground;" +
                "selectionBackground:$TableHeader.hoverBackground;" +
                "selectionInactiveBackground:$TableHeader.hoverBackground;" +
                "selectionForeground:$Table.foreground;");
        scrollPane.getVerticalScrollBar().putClientProperty(FlatClientProperties.STYLE, "" +
                "trackArc:$ScrollBar.thumbArc;" +
                "trackInsets:3,3,3,3;" +
                "thumbInsets:3,3,3,3;" +
                "background:$Table.background;");

        table.getTableHeader().setDefaultRenderer(new TableHeaderAlignment(table) {
            @Override
            protected int getAlignment(int column) {
                return (column == 0) ? SwingConstants.CENTER : SwingConstants.LEADING;
            }
        });

        basicTable = table;
        return panelTable;
    }
    private Component createHardBindTable() {
        JPanel panelTable = new JPanel(new MigLayout("fillx,wrap,insets 15 0 10 0", "[fill]", "[][][fill,grow][]"));

        // Only the columns that matter – no ID, Status, or Approved by
        Object[] columns = new Object[]{"#", "Research Title", "SY-YR", "Applied", "Strand", "Software", "Webpage", "Research Paper"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        JLabel title = new JLabel("Hard Bounded Research Titles");
        title.putClientProperty(FlatClientProperties.STYLE, "font:bold +2");
        panelTable.add(title, "wrap");

        panelTable.add(scrollPane, "grow, push");

        // Animated pagination
        PaginationAnimation pagination = new PaginationAnimation(10, 1, 50);
        JLabel totalLabel = new JLabel("0");

        // Data loader – fetches only the displayed columns
        class HardBindLoader {
            int limit = 20;
            void load(int page) {
                DefaultTableModel m = (DefaultTableModel) table.getModel();
                m.setRowCount(0);
                int offset = (page - 1) * limit;
                try (Connection conn = DBConnection.getMySQLConnection()) {
                    if (conn == null) return;

                    // Count
                    String countSql = "SELECT COUNT(*) FROM aclc_research_titles WHERE record_state = 'ACTIVE' AND Status = 'Hard Bind'";
                    PreparedStatement psCount = conn.prepareStatement(countSql);
                    ResultSet rsCount = psCount.executeQuery();
                    int total = 0;
                    if (rsCount.next()) total = rsCount.getInt(1);
                    totalLabel.setText(DecimalFormat.getInstance().format(total));
                    int totalPages = (int) Math.ceil((double) total / limit);
                    pagination.getModel().setPageRange(page, Math.max(1, totalPages));

                    // Data – no ID, Status, Approved by
                    String sql = "SELECT `Research Title`, `SY-YR`, Applied, Strand, Software, Webpage, `Research Paper` "
                            + "FROM aclc_research_titles "
                            + "WHERE record_state = 'ACTIVE' AND Status = 'Hard Bind' "
                            + "LIMIT ? OFFSET ?";
                    PreparedStatement psData = conn.prepareStatement(sql);
                    psData.setInt(1, limit);
                    psData.setInt(2, offset);
                    ResultSet rs = psData.executeQuery();
                    int rowNum = offset + 1;   // start numbering
                    while (rs.next()) {
                        m.addRow(new Object[]{
                                rowNum++,
                                rs.getString("Research Title"),
                                rs.getString("SY-YR"),
                                rs.getString("Applied"),
                                rs.getString("Strand"),
                                rs.getString("Software"),
                                rs.getString("Webpage"),
                                rs.getString("Research Paper")
                        });
                    }
                } catch (SQLException e) { e.printStackTrace(); }
            }
        }
        HardBindLoader loader = new HardBindLoader();
        pagination.addChangeListener(e -> loader.load(pagination.getSelectedPage()));
        loader.load(1);

        JPanel panelPage = new JPanel(new MigLayout("insets 5 15 5 15", "[][]push[]"));
        panelPage.putClientProperty(FlatClientProperties.STYLE, "background:null;");
        panelPage.add(new JLabel("Total:"));
        panelPage.add(totalLabel);
        panelPage.add(pagination);
        panelTable.add(panelPage);

        // ---------- Column widths ----------
        table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        table.getColumnModel().getColumn(0).setPreferredWidth(40);   // #
        table.getColumnModel().getColumn(0).setMaxWidth(50);
        table.getColumnModel().getColumn(1).setPreferredWidth(350);  // Research Title

        table.getColumnModel().getColumn(2).setPreferredWidth(80);   // SY-YR
        table.getColumnModel().getColumn(2).setMaxWidth(120);

        table.getColumnModel().getColumn(3).setPreferredWidth(90);   // Applied
        table.getColumnModel().getColumn(3).setMaxWidth(120);

        table.getColumnModel().getColumn(4).setPreferredWidth(60);   // Strand
        table.getColumnModel().getColumn(4).setMaxWidth(100);

        table.getColumnModel().getColumn(5).setPreferredWidth(70);   // Software
        table.getColumnModel().getColumn(5).setMaxWidth(100);

        table.getColumnModel().getColumn(6).setPreferredWidth(70);   // Webpage
        table.getColumnModel().getColumn(6).setMaxWidth(100);

        table.getColumnModel().getColumn(7).setPreferredWidth(70);   // Research Paper
        table.getColumnModel().getColumn(7).setMaxWidth(100);

        // Styling (same as before)
        panelTable.putClientProperty(FlatClientProperties.STYLE, "" +
                "arc:10;" +
                "background:$Table.background;");
        table.getTableHeader().putClientProperty(FlatClientProperties.STYLE, "" +
                "height:30;" +
                "hoverBackground:null;" +
                "pressedBackground:null;" +
                "separatorColor:$TableHeader.background;");
        table.putClientProperty(FlatClientProperties.STYLE, "" +
                "rowHeight:30;" +
                "showHorizontalLines:true;" +
                "intercellSpacing:0,1;" +
                "cellFocusColor:$TableHeader.hoverBackground;" +
                "selectionBackground:$TableHeader.hoverBackground;" +
                "selectionInactiveBackground:$TableHeader.hoverBackground;" +
                "selectionForeground:$Table.foreground;");
        scrollPane.getVerticalScrollBar().putClientProperty(FlatClientProperties.STYLE, "" +
                "trackArc:$ScrollBar.thumbArc;" +
                "trackInsets:3,3,3,3;" +
                "thumbInsets:3,3,3,3;" +
                "background:$Table.background;");

        table.getTableHeader().setDefaultRenderer(new TableHeaderAlignment(table) {
            @Override protected int getAlignment(int column) {
                return (column == 0) ? SwingConstants.CENTER : SwingConstants.LEADING;
            }
        });

        return panelTable;
    }
    private Component createHeaderAction() {
        JPanel panel = new JPanel(new MigLayout("insets 5 20 5 20", "[fill,230]push[][]"));

        JTextField txtSectionSearch = new JTextField();
        txtSectionSearch.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Search sections...");
        txtSectionSearch.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON,
                new FlatSVGIcon("raven/modal/demo/icons/search.svg", 0.4f));

        JButton btnCreate = new JButton("Create");
        JButton btnDelete = new JButton("Delete");

        txtSectionSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { loadSections(txtSectionSearch.getText().trim()); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { loadSections(txtSectionSearch.getText().trim()); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { loadSections(txtSectionSearch.getText().trim()); }
        });

        btnCreate.addActionListener(e -> {
            String sectionName = JOptionPane.showInputDialog(this, "Enter section name:");
            if (sectionName != null && !sectionName.trim().isEmpty()) {
                try (Connection conn = DBConnection.getMySQLConnection()) {
                    if (conn == null) return;
                    String sql = "INSERT INTO `best_section_ict_c` (name) VALUES (?)";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setString(1, sectionName.trim());
                        ps.executeUpdate();
                    }
                    showToast("Section created.", "success");
                    loadSections(txtSectionSearch.getText().trim());
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    showToast("Error creating section.", "error");
                }
            }
        });

        btnDelete.addActionListener(e -> {
            JTable table = ((JTable) ((JScrollPane) customTablePanel.getComponent(2)).getViewport().getView());
            DefaultTableModel model = (DefaultTableModel) table.getModel();
            java.util.List<Integer> toDelete = new java.util.ArrayList<>();
            for (int i = 0; i < model.getRowCount(); i++) {
                if ((Boolean) model.getValueAt(i, 0)) {
                    toDelete.add((Integer) model.getValueAt(i, 5));
                }
            }
            if (toDelete.isEmpty()) {
                showToast("Select one or more sections to delete.", "warning");
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(this, "Delete selected section(s)? (Titles will be unlinked)", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                try (Connection conn = DBConnection.getMySQLConnection()) {
                    if (conn == null) return;
                    String sql = "DELETE FROM `best_section_ict_c` WHERE id = ?";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        for (int id : toDelete) {
                            ps.setInt(1, id);
                            ps.addBatch();
                        }
                        ps.executeBatch();
                    }
                    showToast("Selected sections deleted.", "success");
                    loadSections(txtSectionSearch.getText().trim());
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    showToast("Delete failed.", "error");
                }
            }
        });

        panel.add(txtSectionSearch);
        panel.add(btnCreate);
        panel.add(btnDelete);
        panel.putClientProperty(FlatClientProperties.STYLE, "background:null;");
        return panel;
    }

    // --- Inner classes for button rendering/editing ---
    class ButtonRenderer extends JButton implements javax.swing.table.TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
        }
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            setText("View");
            return this;
        }
    }

    class ButtonEditor extends DefaultCellEditor {
        private JButton button;
        private String label;
        private boolean isPushed;
        private int sectionId;

        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton();
            button.setOpaque(true);
            button.addActionListener(e -> fireEditingStopped());
        }

        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            label = "View";
            button.setText(label);
            sectionId = (int) table.getModel().getValueAt(row, 5); // hidden id
            isPushed = true;
            return button;
        }

        public Object getCellEditorValue() {
            if (isPushed) {
                selectedSectionId = sectionId;
                JTable sectionsTable = ((JTable) ((JScrollPane) customTablePanel.getComponent(2)).getViewport().getView());
                String sectionName = (String) sectionsTable.getModel().getValueAt(sectionsTable.getSelectedRow(), 2);
                lbSectionFilter.setText(" (Section: " + sectionName + ")");
                searchTable();
            }
            isPushed = false;
            return label;
        }

        public boolean stopCellEditing() {
            isPushed = false;
            return super.stopCellEditing();
        }
    }

    // --- Fields ---
    private int limit = 50;
    private JPagination pagination;
    private JTable basicTable;
    private JLabel lbTotalPage;
    private JTextField txtBasicSearch;
    private String currentSearch = "";
    private int selectedSectionId = -1;
    private JLabel lbSectionFilter;
    private JPanel customTablePanel;
}