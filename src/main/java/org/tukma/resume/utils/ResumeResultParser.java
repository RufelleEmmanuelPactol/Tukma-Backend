package org.tukma.resume.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class to parse Python-formatted resume analysis results
 */
public class ResumeResultParser {

    private static final Pattern KEYWORD_PATTERN = Pattern.compile("'([^']+)': \\{(.+?)\\}");
    private static final Pattern SCORE_PATTERN = Pattern.compile("'similarity_score': np\\.float64\\(([0-9.]+)\\)");
    private static final Pattern NGRAM_PATTERN = Pattern.compile("'best_matching_ngram': '([^']+)'");

    /**
     * Parses a Python-formatted result string into a Java Map
     * 
     * @param pythonResult The Python-formatted result string
     * @return Map representation of the result
     */
    public static Map<String, Map<String, Object>> parseResults(String pythonResult) {
        Map<String, Map<String, Object>> results = new HashMap<>();
        
        // Remove the outer quotes and braces if present
        pythonResult = pythonResult.trim();
        if (pythonResult.startsWith("'") && pythonResult.endsWith("'")) {
            pythonResult = pythonResult.substring(1, pythonResult.length() - 1);
        }
        
        // Extract each keyword section
        Matcher keywordMatcher = KEYWORD_PATTERN.matcher(pythonResult);
        while (keywordMatcher.find()) {
            String keyword = keywordMatcher.group(1);
            String keywordData = keywordMatcher.group(2);
            
            Map<String, Object> keywordResults = new HashMap<>();
            
            // Extract similarity score
            Matcher scoreMatcher = SCORE_PATTERN.matcher(keywordData);
            if (scoreMatcher.find()) {
                double score = Double.parseDouble(scoreMatcher.group(1));
                keywordResults.put("similarity_score", score);
            }
            
            // Extract best matching ngram
            Matcher ngramMatcher = NGRAM_PATTERN.matcher(keywordData);
            if (ngramMatcher.find()) {
                String ngram = ngramMatcher.group(1);
                keywordResults.put("best_matching_ngram", ngram);
            }
            
            results.put(keyword, keywordResults);
        }
        
        return results;
    }
    
    /**
     * Converts the parsed result map to JSON string
     * 
     * @param results Parsed result map
     * @return JSON string representation
     */
    public static String toJsonString(Map<String, Map<String, Object>> results) {
        try {
            return new Gson().toJson(results);
        } catch (Exception e) {
            return "{}";
        }
    }
    
    /**
     * Parses a Python-formatted result string directly to JSON
     * 
     * @param pythonResult The Python-formatted result string
     * @return JSON string representation
     */
    public static String pythonToJson(String pythonResult) {
        return toJsonString(parseResults(pythonResult));
    }
}
