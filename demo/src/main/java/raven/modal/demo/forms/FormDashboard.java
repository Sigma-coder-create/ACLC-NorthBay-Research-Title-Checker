package raven.modal.demo.forms;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.util.UIScale;
import net.miginfocom.swing.MigLayout;
import raven.modal.demo.component.ToolBarSelection;
import raven.modal.demo.component.chart.*;
import raven.modal.demo.component.chart.themes.ColorThemes;
import raven.modal.demo.component.chart.themes.DefaultChartTheme;
import raven.modal.demo.component.chart.utils.ToolBarTimeSeriesChartRenderer;
import raven.modal.demo.component.dashboard.CardBox;
import raven.modal.demo.system.Form;
import raven.modal.demo.utils.SystemForm;
import java.text.SimpleDateFormat;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.DateTickUnit;
import org.jfree.chart.axis.DateTickUnitType;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;

import com.finals.db.DBConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;

@SystemForm(name = "Dashboard", description = "dashboard form display some details")
public class FormDashboard extends Form {

    public FormDashboard() {
        init();
    }

    private void init() {
        setLayout(new MigLayout("wrap,fill", "[fill]", "[grow 0][fill]"));
        createTitle();
        createPanelLayout();
        createCard();
        createRecentTable();
       // createChart();
        // createOtherChart();   // commented out – not used
    }

    @Override
    public void formInit() {
        loadData();
    }

    @Override
    public void formRefresh() {
        loadData();
    }

    private void loadData() {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            private int total = 0, pending = 0, approved = 0, denied = 0;
            private org.jfree.data.time.TimeSeriesCollection chartDataset;

            @Override
            protected Void doInBackground() throws Exception {
                try (Connection conn = DBConnection.getMySQLConnection()) {
                    if (conn == null) return null;

                    // ---- Card statistics ----
                    String countSql = "SELECT "
                            + "COUNT(*) AS total, "
                            + "SUM(CASE WHEN Status = 'Pending' THEN 1 ELSE 0 END) AS pending, "
                            + "SUM(CASE WHEN Status = 'Approved' THEN 1 ELSE 0 END) AS approved, "
                            + "SUM(CASE WHEN Status = 'Denied' THEN 1 ELSE 0 END) AS denied "
                            + "FROM aclc_research_titles WHERE record_state = 'ACTIVE'";
                    try (PreparedStatement ps = conn.prepareStatement(countSql);
                         ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            total = rs.getInt("total");
                            pending = rs.getInt("pending");
                            approved = rs.getInt("approved");
                            denied = rs.getInt("denied");
                        }
                    }

                    // ---- Recent submissions (last 5) ----
                    recentModel.setRowCount(0);
                    String recentSql = "SELECT `Research Title`, `SY-YR`, Status, `Approved by` "
                            + "FROM aclc_research_titles WHERE record_state = 'ACTIVE' "
                            + "ORDER BY last_updated DESC LIMIT 5";
                    try (PreparedStatement ps = conn.prepareStatement(recentSql);
                         ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            recentModel.addRow(new Object[]{
                                    rs.getString("Research Title"),
                                    rs.getString("SY-YR"),
                                    rs.getString("Status"),
                                    rs.getString("Approved by")
                            });
                        }
                    }

                    // ---- Build approved/denied time series ----
                    chartDataset = buildApprovalDeniedDataset(conn);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void done() {
                DecimalFormat fmt = new DecimalFormat("#,###");
                cardBox.setValueAt(0, fmt.format(total), "Total Research Titles", "", true);
                cardBox.setValueAt(1, fmt.format(pending), "Pending", "", false);
                cardBox.setValueAt(2, fmt.format(approved), "Approved", "", true);
                cardBox.setValueAt(3, fmt.format(denied), "Denied", "", false);

                if (chartDataset != null && timeSeriesChart != null) {
                    timeSeriesChart.setDataset(chartDataset);

                    org.jfree.chart.JFreeChart chart = timeSeriesChart.getFreeChart();
                    chart.setTitle("Approved vs Denied Over Time");

                    if (chart.getXYPlot() != null) {
                        org.jfree.chart.axis.ValueAxis axis = chart.getXYPlot().getRangeAxis();
                        if (axis instanceof org.jfree.chart.axis.NumberAxis) {
                            org.jfree.chart.axis.NumberAxis numAxis = (org.jfree.chart.axis.NumberAxis) axis;
                            numAxis.setNumberFormatOverride(new DecimalFormat("#,###"));
                        }
                    }
                }
            

                // All other charts are now commented out – no sample data loaded
                // candlestickChart.setDataset(SampleData.getOhlcDataset());
                // barChart.setDataset(SampleData.getCategoryDataset());
                // spiderChart.setDataset(SampleData.getCategoryDataset());
                // pieChart.setDataset(SampleData.getPieDataset());
            }
        };
        worker.execute();
    }

    private org.jfree.data.time.TimeSeriesCollection buildApprovalDeniedDataset(Connection conn) throws SQLException {
        org.jfree.data.time.TimeSeries approvedSeries = new org.jfree.data.time.TimeSeries("Approved");
        org.jfree.data.time.TimeSeries deniedSeries  = new org.jfree.data.time.TimeSeries("Denied");

        String sql = "SELECT YEAR(last_updated) AS yr, MONTH(last_updated) AS mth, " +
                    "SUM(CASE WHEN Status = 'Approved' THEN 1 ELSE 0 END) AS approved_count, " +
                    "SUM(CASE WHEN Status = 'Denied' THEN 1 ELSE 0 END) AS denied_count " +
                    "FROM aclc_research_titles " +
                    "WHERE record_state = 'ACTIVE' " +
                    "GROUP BY yr, mth " +
                    "ORDER BY yr, mth";

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int year  = rs.getInt("yr");
                int month = rs.getInt("mth");
                org.jfree.data.time.Month m = new org.jfree.data.time.Month(month, year);
                approvedSeries.add(m, rs.getInt("approved_count"));
                deniedSeries.add(m, rs.getInt("denied_count"));
            }
        }

        org.jfree.data.time.TimeSeriesCollection dataset = new org.jfree.data.time.TimeSeriesCollection();
        dataset.addSeries(approvedSeries);
        dataset.addSeries(deniedSeries);
        return dataset;
    }

    private void createTitle() {
        JPanel panel = new JPanel(new MigLayout("fillx", "[]push[][]"));
        JLabel title = new JLabel("Dashboard");
        title.putClientProperty(FlatClientProperties.STYLE, "font:bold +3");

            ToolBarSelection<ColorThemes> toolBarSelection = new ToolBarSelection<>(ColorThemes.values(), colorThemes -> {
            if (DefaultChartTheme.setChartColors(colorThemes)) {
                if (timeSeriesChart != null) {
                    DefaultChartTheme.applyTheme(timeSeriesChart.getFreeChart());
                    // DefaultChartTheme.applyTheme(candlestickChart.getFreeChart());  // commented out
                    // DefaultChartTheme.applyTheme(barChart.getFreeChart());          // commented out
                    // DefaultChartTheme.applyTheme(pieChart.getFreeChart());          // commented out
                    // DefaultChartTheme.applyTheme(spiderChart.getFreeChart());       // commented out
                    cardBox.setCardIconColor(0, DefaultChartTheme.getColor(0));
                    cardBox.setCardIconColor(1, DefaultChartTheme.getColor(1));
                    cardBox.setCardIconColor(2, DefaultChartTheme.getColor(2));
                    cardBox.setCardIconColor(3, DefaultChartTheme.getColor(3));
                }
            }
        });
        panel.add(title);
        panel.add(toolBarSelection);
        add(panel);
    }

    private void createPanelLayout() {
        panelLayout = new JPanel(new DashboardLayout());
        JScrollPane scrollPane = new JScrollPane(panelLayout);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(10);
        scrollPane.getVerticalScrollBar().putClientProperty(FlatClientProperties.STYLE, "" +
                "width:5;" +
                "trackArc:$ScrollBar.thumbArc;" +
                "trackInsets:0,0,0,0;" +
                "thumbInsets:0,0,0,0;");
        add(scrollPane);
    }

    private void createCard() {
        JPanel panel = new JPanel(new MigLayout("fillx", "[fill]"));
        cardBox = new CardBox();
        cardBox.addCardItem(createIcon("raven/modal/demo/icons/dashboard/customer.svg", DefaultChartTheme.getColor(0)), "");
        cardBox.addCardItem(createIcon("raven/modal/demo/icons/dashboard/income.svg", DefaultChartTheme.getColor(1)), "");
        cardBox.addCardItem(createIcon("raven/modal/demo/icons/dashboard/expense.svg", DefaultChartTheme.getColor(2)), "");
        cardBox.addCardItem(createIcon("raven/modal/demo/icons/dashboard/profit.svg", DefaultChartTheme.getColor(3)), "");
        panel.add(cardBox);
        panelLayout.add(panel);
    }

    private void createRecentTable() {
        JPanel panel = new JPanel(new MigLayout("fillx,wrap,insets 10 0 0 0", "[fill]", "[][250]"));
        JLabel lbl = new JLabel("Recent Submissions");
        lbl.putClientProperty(FlatClientProperties.STYLE, "font:bold +2");
        panel.add(lbl);

        recentModel = new DefaultTableModel(new Object[]{"Title", "SY‑YR", "Status", "Approved by"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        recentTable = new JTable(recentModel);
        JScrollPane scrollPane = new JScrollPane(recentTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        recentTable.setRowHeight(28);
        recentTable.getTableHeader().putClientProperty(FlatClientProperties.STYLE, "" +
                "height:30;hoverBackground:null;pressedBackground:null;");
        recentTable.putClientProperty(FlatClientProperties.STYLE, "" +
                "showHorizontalLines:true;intercellSpacing:0,1;");
        scrollPane.getVerticalScrollBar().putClientProperty(FlatClientProperties.STYLE, "" +
                "width:5;trackArc:$ScrollBar.thumbArc;");
        panel.add(scrollPane, "grow, push");
        panelLayout.add(panel);
    }

    private void createChart() {
        JPanel panel = new JPanel(new MigLayout("gap 14,wrap,fillx", "[fill]", "[650]"));
        timeSeriesChart = new TimeSeriesChart();

        // --- Remove the broken mouse listener (prevents ClassCastException flood) ---
        org.jfree.chart.ChartPanel cp = timeSeriesChart.getChartPanel();
        if (cp != null) {
            for (java.util.EventListener el : cp.getListeners(org.jfree.chart.ChartMouseListener.class)) {
                cp.removeChartMouseListener((org.jfree.chart.ChartMouseListener) el);
            }
        }
        // candlestickChart and barChart are no longer created or displayed
        // candlestickChart = new CandlestickChart();
        // barChart = new BarChart();

        timeSeriesChart.add(new ToolBarTimeSeriesChartRenderer(timeSeriesChart), "al trailing,grow 0", 0);
        panel.add(timeSeriesChart);
        // panel.add(candlestickChart);
        // panel.add(barChart);
        panelLayout.add(panel);
    }

    // private void createOtherChart() { … }   // not used, can be removed or left commented out

    private Icon createIcon(String icon, Color color) {
        return new FlatSVGIcon(icon, 0.4f).setColorFilter(new FlatSVGIcon.ColorFilter(color1 -> color));
    }

    // --- Fields ---
    private JPanel panelLayout;
    private CardBox cardBox;
    private TimeSeriesChart timeSeriesChart;

    // These are still declared to avoid compile errors if referenced elsewhere,
    // but they are never instantiated and should be safe as null.
    // private CandlestickChart candlestickChart;
    // private BarChart barChart;
    // private SpiderChart spiderChart;
    // private PieChart pieChart;

    private JTable recentTable;
    private DefaultTableModel recentModel;

    // --- DashboardLayout (unchanged) ---
    private class DashboardLayout implements LayoutManager {
        private int gap = 0;
        @Override public void addLayoutComponent(String name, Component comp) {}
        @Override public void removeLayoutComponent(Component comp) {}

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            synchronized (parent.getTreeLock()) {
                Insets insets = parent.getInsets();
                int width = insets.left + insets.right;
                int height = insets.top + insets.bottom;
                int g = UIScale.scale(gap);
                int count = parent.getComponentCount();
                for (int i = 0; i < count; i++) {
                    height += parent.getComponent(i).getPreferredSize().height;
                }
                if (count > 1) height += (count - 1) * g;
                return new Dimension(width, height);
            }
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            return new Dimension(10, 10);
        }

        @Override
        public void layoutContainer(Container parent) {
            synchronized (parent.getTreeLock()) {
                Insets insets = parent.getInsets();
                int x = insets.left, y = insets.top;
                int width = parent.getWidth() - (insets.left + insets.right);
                int g = UIScale.scale(gap);
                int count = parent.getComponentCount();
                for (int i = 0; i < count; i++) {
                    Component com = parent.getComponent(i);
                    Dimension size = com.getPreferredSize();
                    com.setBounds(x, y, width, size.height);
                    y += size.height + g;
                }
            }
        }
    }
}