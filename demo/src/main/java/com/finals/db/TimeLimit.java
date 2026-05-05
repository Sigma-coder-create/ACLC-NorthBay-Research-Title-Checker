package com.finals.db;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.awt.Color;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class TimeLimit {
    public String title;
    public double score;
    public String source;
    public String dateStr;
    public boolean isRecent;
    public int id;   

    public TimeLimit(String title, double score, String source, String dateStr, int id) {
        this.title = title;
        this.score = score;
        this.source = source;
        this.dateStr = dateStr;
        this.id = id;
        this.isRecent = calculateRecency(dateStr);
    }

    /** Cleanup only on MySQL */
    public static void cleanupExpiredTitles() {
        int currentYear = java.time.Year.now().getValue();
        int cutoffYear = currentYear - 5;

        String selectSQL = "SELECT ID, `SY-YR` FROM aclc_research_titles WHERE record_state = 'ACTIVE'";
        String updateSQL = "UPDATE aclc_research_titles SET record_state = 'DELETED' WHERE ID = ?";

        try (Connection con = DBConnection.getMySQLConnection()) {
            if (con == null) return;

            List<Integer> toDelete = new ArrayList<>();
            try (PreparedStatement ps = con.prepareStatement(selectSQL);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String syyr = rs.getString("SY-YR");
                    if (syyr == null || syyr.isEmpty()) continue;
                    try {
                        int endYear = Integer.parseInt(syyr.split("-")[1].trim());
                        if (endYear <= cutoffYear) {
                            toDelete.add(rs.getInt("ID"));
                        }
                    } catch (NumberFormatException e) { /* ignore */ }
                }
            }

            if (!toDelete.isEmpty()) {
                try (PreparedStatement ps = con.prepareStatement(updateSQL)) {
                    for (int id : toDelete) {
                        ps.setInt(1, id);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                System.out.println("[Cleanup] Removed " + toDelete.size() + " expired titles from MySQL");
            }
        } catch (Exception e) {
            System.err.println("Cleanup failed: " + e.getMessage());
        }
    }

            private boolean calculateRecency(String date) {
        if (date == null || date.isEmpty()) return true;
        try {
            int currentYear = LocalDate.now().getYear();
            int cutoffYear = currentYear - 5; // e.g., 2026 - 5 = 2021

            // 1. Handle Full Date format (e.g., 4/26/2021)
            if (date.contains("/")) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yyyy");
                LocalDate researchDate = LocalDate.parse(date, formatter);
                // Based on your requirement: if today is 4/27/2026, 4/26/2021 is still allowed
                // We check if the year is at least the cutoff year
                return researchDate.getYear() >= cutoffYear;
            }

            // 2. Handle SY-YR format (e.g., 2020-2021)
            if (date.contains("-")) {
                String[] years = date.split("-");
                int endYear = Integer.parseInt(years[1].trim());
                return endYear >= cutoffYear;
            }

            // 3. Handle Simple Year format (e.g., 2021)
            return Integer.parseInt(date.trim()) >= cutoffYear;

        } catch (Exception e) {
            return true; // If we can't read the date, don't block it
        }
    }

    public Color getStatusColor() {
        if (!isRecent) return Color.GRAY; // Expired
        if ("Web".equals(source)) return new Color(0, 150, 0); // Green for Web
        return score > 0.75 ? Color.RED : new Color(100, 100, 255); // Red/Blue for DB
    }

    public String getDisplayText() {
        String base = (int)(score * 100) + "% [" + source + "]";
        return isRecent ? base : base + " - EXPIRED";
    }
}