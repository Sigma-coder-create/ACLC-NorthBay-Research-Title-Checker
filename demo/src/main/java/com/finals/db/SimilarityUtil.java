package com.finals.db;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import static raven.modal.demo.utils.DemoPreferences.isSuggestionsEnabled;

import raven.modal.demo.utils.DemoPreferences;

public class SimilarityUtil {

    private static final Set<String> STOPWORDS = new HashSet<>(Arrays.asList(
        "the","a","an","of","and","in","for","to","on","by","with","from","using","based","as","is","are"
    ));

    private static final Map<String, String> SYNONYMS = new HashMap<>();
    static {
        SYNONYMS.put("mitigate","reduce");
        SYNONYMS.put("reduction","reduce");
        SYNONYMS.put("effect","impact");
        SYNONYMS.put("influence","impact");
        SYNONYMS.put("development","design");
        SYNONYMS.put("evaluation","assessment");
        SYNONYMS.put("model","framework");
        SYNONYMS.put("analysis","study");
        SYNONYMS.put("system","platform");
        SYNONYMS.put("application","app");
        SYNONYMS.put("implementation","deployment");
        SYNONYMS.put("detection","recognition");
        SYNONYMS.put("recognition","detection");
    }

    private static final Set<String> BASE_KEYWORDS = new HashSet<>(Arrays.asList(
        "design","development","system","analysis","implementation","management","evaluation","application",
        "performance","automation","model","framework","simulation","monitoring","process","technology",
        "software","hardware","algorithm","database","mobile","web","network","security","optimization",
        "recognition","learning","artificial","intelligence","data","prediction","classification",
        "detection","tracking","research","title","duplication","education","attendance","student","teacher"
    ));

    private static final Map<String, Double> idf = new HashMap<>();
    private static final Set<String> vocabulary = new HashSet<>();
    private static final Map<String, Double> learnedKeywordWeights = new HashMap<>();
    private static int docCount = 0;
    private static boolean initialized = false;

    /** Pulls titles ONLY from MySQL */
    public static synchronized void initializeFromDatabases() {
        Map<String, Integer> docFreq = new HashMap<>();
        Map<String, Integer> termTotals = new HashMap<>();
        docCount = 0;

        List<String> allTitles = new ArrayList<>();

        try (Connection mysql = DBConnection.getMySQLConnection()) {
            if (mysql != null) collectTitlesFromConn(mysql, allTitles);
        } catch (Exception e) {
            System.err.println("[SimilarityUtil] DB read error: " + e.getMessage());
        }
        for (String title : allTitles) {
            docCount++;
            Set<String> seenInDoc = new HashSet<>();
            String norm = normalize(title);
            if (norm.isEmpty()) continue;
            for (String w : norm.split("\\s+")) {
                if (w.length() <= 2 || STOPWORDS.contains(w)) continue;
                w = applySynonym(w);
                vocabulary.add(w);
                termTotals.put(w, termTotals.getOrDefault(w, 0) + 1);
                if (!seenInDoc.contains(w)) {
                    docFreq.put(w, docFreq.getOrDefault(w, 0) + 1);
                    seenInDoc.add(w);
                }
            }
        }

        if (docCount == 0) docCount = 1;

        for (String word : vocabulary) {
            int df = docFreq.getOrDefault(word, 1);
            double value = Math.log((double) docCount / (double) df) + 1.0;
            idf.put(word, value);
            double base = BASE_KEYWORDS.contains(word) ? 1.2 : 1.0;
            double learned = 0.8 + (0.4 * ((double) termTotals.getOrDefault(word,0) / (double) Math.max(1, Collections.max(termTotals.values()))));
            learnedKeywordWeights.put(word, base * learned);
        }

        for (String bk : BASE_KEYWORDS) vocabulary.add(bk);
        initialized = true;
        System.out.println("[SimilarityUtil] Initialized vocabulary size=" + vocabulary.size() + " documents=" + docCount);
    }

    /** Now queries MySQL directly */
    public static List<TimeLimit> getDetailedSimilarTitles(String inputTitle) {
        List<TimeLimit> results = new ArrayList<>();
        String sql = "SELECT `Research Title`, `SY-YR` FROM ACLC_research_titles";

        try (Connection con = DBConnection.getMySQLConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String title = rs.getString("Research Title");
                String dateStr = rs.getString("SY-YR");
                double score = calculateSimilarity(inputTitle, title);
                if (score > 0.4) {
                    results.add(new TimeLimit(title, score, "Database", dateStr));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return results;
    }
    public static List<String> getSimilarTitlesFromWeb(String inputTitle) {
        List<String> webTitles = new ArrayList<>();
        if (!DemoPreferences.isSuggestionsEnabled() || !DBConnection.isInternetAvailable()) {
            return webTitles;
        }

        try {
        String encoded = java.net.URLEncoder.encode(inputTitle, "UTF-8");
            java.net.URL url = new java.net.URL("https://scholar.google.com/scholar?q=" + encoded);

            if (java.awt.Desktop.isDesktopSupported()) {
                // Convert URL to URI here
                java.awt.Desktop.getDesktop().browse(url.toURI()); 
            }

        } catch (Exception e) {
            System.err.println("Web Check Failed: " + e.getMessage());
        }
        return webTitles;
    }
    private static void collectTitlesFromConn(Connection conn, List<String> out) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT `Research Title` FROM ACLC_research_titles");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String t = rs.getString(1);
                if (t != null && !t.trim().isEmpty()) out.add(t);
            }
        } catch (SQLException e) {
            System.err.println("[SimilarityUtil] collectTitlesFromConn error: " + e.getMessage());
        }
    }

    public static synchronized void onNewTitleAdded(String title) {
        if (title == null || title.trim().isEmpty()) return;
        String norm = normalize(title);
        if (norm.isEmpty()) return;
        Set<String> seen = new HashSet<>();
        docCount++;
        for (String w : norm.split("\\s+")) {
            if (w.length() <= 2 || STOPWORDS.contains(w)) continue;
            w = applySynonym(w);
            vocabulary.add(w);
            learnedKeywordWeights.put(w, learnedKeywordWeights.getOrDefault(w, 0.8) + 0.2);
            if (!seen.contains(w)) {
                idf.put(w, Math.log((double) docCount / (double) (1 + getDocFreqApprox(w))) + 1.0);
                seen.add(w);
            }
        }
    }

    private static int getDocFreqApprox(String word) {
        double weight = learnedKeywordWeights.getOrDefault(word, 0.0);
        return (int) Math.max(1, Math.round(weight * docCount / 1.5));
    }

    public static double similarityScore(String a, String b) {
        if (!initialized) initializeFromDatabases();

        String s1 = normalize(applySynonymsToText(a));
        String s2 = normalize(applySynonymsToText(b));
        double lev = levenshteinSimilarity(s1, s2);
        double cosine = tfidfCosine(s1, s2);
        double kw = weightedKeywordOverlap(s1, s2);
        double finalScore = (0.4 * lev) + (0.4 * cosine) + (0.2 * kw);
        return Math.max(0.0, Math.min(1.0, finalScore));
    }

    private static String applySynonymsToText(String text) {
        StringBuilder sb = new StringBuilder();
        for (String w : text.toLowerCase().replaceAll("[^a-z0-9 ]", " ").split("\\s+")) {
            if (w.isEmpty()) continue;
            if (STOPWORDS.contains(w)) continue;
            sb.append(applySynonym(w)).append(" ");
        }
        return sb.toString().trim();
    }

    private static String applySynonym(String w) {
        String mapped = SYNONYMS.getOrDefault(w, w);
        return mapped;
    }

    private static String normalize(String s) {
        return Arrays.stream(s.toLowerCase().replaceAll("[^a-z0-9 ]", " ").split("\\s+"))
                .filter(tok -> !tok.isEmpty() && !STOPWORDS.contains(tok))
                .collect(Collectors.joining(" "))
                .trim();
    }

    private static double levenshteinSimilarity(String s1, String s2) {
        int max = Math.max(s1.length(), s2.length());
        if (max == 0) return 1.0;
        int d = levenshteinDistance(s1, s2);
        return 1.0 - ((double) d / (double) max);
    }

    private static int levenshteinDistance(String s1, String s2) {
        int[] costs = new int[s2.length() + 1];
        for (int j = 0; j <= s2.length(); j++) costs[j] = j;
        for (int i = 1; i <= s1.length(); i++) {
            costs[0] = i;
            int nw = i - 1;
            for (int j = 1; j <= s2.length(); j++) {
                int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]),
                        s1.charAt(i - 1) == s2.charAt(j - 1) ? nw : nw + 1);
                nw = costs[j];
                costs[j] = cj;
            }
        }
        return costs[s2.length()];
    }

    private static double tfidfCosine(String s1, String s2) {
        Map<String, Double> v1 = tfidfVector(s1);
        Map<String, Double> v2 = tfidfVector(s2);
        Set<String> all = new HashSet<>();
        all.addAll(v1.keySet());
        all.addAll(v2.keySet());
        double dot = 0, mag1 = 0, mag2 = 0;
        for (String k : all) {
            double x = v1.getOrDefault(k, 0.0);
            double y = v2.getOrDefault(k, 0.0);
            dot += x * y;
            mag1 += x * x;
            mag2 += y * y;
        }
        if (mag1 == 0 || mag2 == 0) return 0.0;
        return dot / (Math.sqrt(mag1) * Math.sqrt(mag2));
    }

    private static Map<String, Double> tfidfVector(String s) {
        Map<String, Integer> tf = new HashMap<>();
        for (String w : s.split("\\s+")) {
            if (w.length() <= 2) continue;
            tf.put(w, tf.getOrDefault(w, 0) + 1);
        }
        Map<String, Double> vec = new HashMap<>();
        for (Map.Entry<String, Integer> e : tf.entrySet()) {
            String w = e.getKey();
            double termFreq = e.getValue();
            double idfVal = idf.getOrDefault(w, Math.log((double) Math.max(1, docCount) / 1.0) + 1.0);
            double weight = termFreq * idfVal * learnedKeywordWeights.getOrDefault(w, 1.0);
            vec.put(w, weight);
        }
        return vec;
    }

    private static double weightedKeywordOverlap(String s1, String s2) {
        Set<String> a = Arrays.stream(s1.split("\\s+")).collect(Collectors.toSet());
        Set<String> b = Arrays.stream(s2.split("\\s+")).collect(Collectors.toSet());
        double score = 0;
        for (String w : a) {
            if (b.contains(w)) {
                score += learnedKeywordWeights.getOrDefault(w, 1.0);
            }
        }
        double denom = Math.max(1, Math.max(a.size(), b.size()));
        return Math.min(1.0, score / denom);
    }

    public static double calculateSimilarity(String a, String b) {
        if (a == null || b == null) return 0.0;
        return similarityScore(a, b);
    }
}