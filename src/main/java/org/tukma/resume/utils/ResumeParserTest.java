package org.tukma.resume.utils;

import java.util.Map;

/**
 * Simple test class to demonstrate the Resume Result Parser functionality
 */
public class ResumeParserTest {
    
    public static void main(String[] args) {
        // Example resume result string from the microservice
        String pythonResult = "{'javascript': {'similarity_score': np.float64(0.48658517708123633), 'best_matching_ngram': 'in Laravel, JavaScript,'}, " +
                "'frontend': {'similarity_score': np.float64(0.6945115131278724), 'best_matching_ngram': 'the frontend and'}, " +
                "'software engineer': {'similarity_score': np.float64(0.6592739827568643), 'best_matching_ngram': 'Information Technology Engineers'}}";
        
        // Parse the Python-formatted string to a Java Map
        Map<String, Map<String, Object>> parsedResults = ResumeResultParser.parseResults(pythonResult);
        
        // Print the parsed results
        System.out.println("Parsed Results:");
        for (Map.Entry<String, Map<String, Object>> entry : parsedResults.entrySet()) {
            String keyword = entry.getKey();
            Map<String, Object> data = entry.getValue();
            
            System.out.println("Keyword: " + keyword);
            System.out.println("  - Similarity Score: " + data.get("similarity_score"));
            System.out.println("  - Best Matching Ngram: " + data.get("best_matching_ngram"));
        }
        
        // Convert to JSON
        String json = ResumeResultParser.toJsonString(parsedResults);
        System.out.println("\nJSON representation:");
        System.out.println(json);
    }
}
