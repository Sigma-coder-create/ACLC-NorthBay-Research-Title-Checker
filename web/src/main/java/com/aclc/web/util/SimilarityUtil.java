package com.aclc.web.util;

import java.util.*;
import java.util.stream.Collectors;

public class SimilarityUtil {

    // ---- Stop‑words & synonyms (identical to desktop) ----
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

    private Map<String, Double> idf = new HashMap<>();
    private Set<String> vocabulary = new HashSet<>();
    private Map<String, Double> learnedKeywordWeights = new HashMap<>();
    private int docCount = 0;
    private boolean initialized = false;

    /**
     * Initialise the TF‑IDF model using a list of all active titles (excluding Hard Bind).
     * Call this once after the application starts (e.g. via @PostConstruct in the service).
     */
    public void initialize(List<String> allTitles) {
        Map<String, Integer> docFreq = new HashMap<>();
        Map<String, Integer> termTotals = new HashMap<>();
        docCount = allTitles.size();

        for (String title : allTitles) {
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
            double learned = 0.8 + (0.4 * ((double) termTotals.getOrDefault(word,0) /
                    (double) Math.max(1, Collections.max(termTotals.values()))));
            learnedKeywordWeights.put(word, base * learned);
        }

        for (String bk : BASE_KEYWORDS) vocabulary.add(bk);
        initialized = true;
    }

    /**
     * Public entry point: the same composite score as the desktop app.
     */
    public double calculateSimilarity(String a, String b) {
        if (a == null || b == null) return 0.0;
        if (!initialized) throw new RuntimeException("SimilarityUtil not initialized");
        return similarityScore(a, b);
    }

    // ----- Helper methods (identical to desktop) -----
    private double similarityScore(String a, String b) {
        String s1 = normalize(applySynonymsToText(a));
        String s2 = normalize(applySynonymsToText(b));
        double lev = levenshteinSimilarity(s1, s2);
        double cosine = tfidfCosine(s1, s2);
        double kw = weightedKeywordOverlap(s1, s2);
        double finalScore = (0.4 * lev) + (0.4 * cosine) + (0.2 * kw);
        return Math.max(0.0, Math.min(1.0, finalScore));
    }

    private String applySynonymsToText(String text) {
        StringBuilder sb = new StringBuilder();
        for (String w : text.toLowerCase().replaceAll("[^a-z0-9 ]", " ").split("\\s+")) {
            if (w.isEmpty()) continue;
            if (STOPWORDS.contains(w)) continue;
            sb.append(applySynonym(w)).append(" ");
        }
        return sb.toString().trim();
    }

    private String applySynonym(String w) {
        return SYNONYMS.getOrDefault(w, w);
    }

    private String normalize(String s) {
        return Arrays.stream(s.toLowerCase().replaceAll("[^a-z0-9 ]", " ").split("\\s+"))
                .filter(tok -> !tok.isEmpty() && !STOPWORDS.contains(tok))
                .collect(Collectors.joining(" "))
                .trim();
    }

    private double levenshteinSimilarity(String s1, String s2) {
        int max = Math.max(s1.length(), s2.length());
        if (max == 0) return 1.0;
        int d = levenshteinDistance(s1, s2);
        return 1.0 - ((double) d / (double) max);
    }

    private int levenshteinDistance(String s1, String s2) {
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

    private double tfidfCosine(String s1, String s2) {
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

    private Map<String, Double> tfidfVector(String s) {
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

    private double weightedKeywordOverlap(String s1, String s2) {
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
}