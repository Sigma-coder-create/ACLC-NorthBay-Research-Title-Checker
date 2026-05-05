package raven.modal.demo.forms;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import raven.modal.demo.system.Form;
import raven.modal.demo.utils.SystemForm;

import javax.swing.*;
import java.awt.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;

@SystemForm(name = "Calendar", description = "Monthly calendar view")
public class FormCalendar extends Form {

    private YearMonth currentMonth;
    private JPanel daysPanel;
    private JLabel monthLabel;

    public FormCalendar() {
        currentMonth = YearMonth.now();
        init();
    }

    private void init() {
        setLayout(new MigLayout("wrap, fill, insets 20", "[grow]", "[][][grow]"));

        // Navigation
        JPanel navPanel = new JPanel(new MigLayout("insets 0", "[][]push[]"));
        JButton prev = new JButton("<");
        JButton next = new JButton(">");
        monthLabel = new JLabel("", SwingConstants.CENTER);
        monthLabel.putClientProperty(FlatClientProperties.STYLE, "font:bold +2;");

        prev.addActionListener(e -> {
            currentMonth = currentMonth.minusMonths(1);
            updateCalendar();
        });
        next.addActionListener(e -> {
            currentMonth = currentMonth.plusMonths(1);
            updateCalendar();
        });

        navPanel.add(prev);
        navPanel.add(monthLabel);
        navPanel.add(next);
        add(navPanel, "growx");

        // Day‑of‑week headers
        daysPanel = new JPanel(new GridLayout(0, 7, 2, 2));
        add(daysPanel, "grow, pushy");

        updateCalendar();
    }

    private void updateCalendar() {
        monthLabel.setText(currentMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + currentMonth.getYear());
        daysPanel.removeAll();

        // Header row – day names
        DayOfWeek[] days = DayOfWeek.values();
        for (DayOfWeek dow : days) {
            JLabel lbl = new JLabel(dow.getDisplayName(TextStyle.SHORT, Locale.getDefault()), SwingConstants.CENTER);
            lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
            daysPanel.add(lbl);
        }

        // First day of month and total days
        LocalDate firstDay = currentMonth.atDay(1);
        int totalDays = currentMonth.lengthOfMonth();
        int startDay = firstDay.getDayOfWeek().getValue() - 1; // Monday = 0

        // Empty cells before start
        for (int i = 0; i < startDay; i++) {
            daysPanel.add(new JLabel(""));
        }

        // Day cells
        for (int day = 1; day <= totalDays; day++) {
            JLabel lbl = new JLabel(String.valueOf(day), SwingConstants.CENTER);
            lbl.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
            lbl.setOpaque(true);
            lbl.setBackground(Color.WHITE);
            daysPanel.add(lbl);
        }

        daysPanel.revalidate();
        daysPanel.repaint();
    }
}