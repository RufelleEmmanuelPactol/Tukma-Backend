package org.tukma.interview.services;

import org.springframework.stereotype.Service;
import org.tukma.interview.models.Message;

import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.logging.Logger;
import java.util.Map;
import java.util.HashMap;

import com.google.gson.Gson;

@Service
public class MessageProcessingService {

    private final Environment environment;
    private static final Logger logger = Logger.getLogger(MessageProcessingService.class.getName());
    
    public MessageProcessingService(Environment environment) {
        this.environment = environment;
    }
    
    /**
     * Helper method to strip markdown code block syntax from a string
     * 
     * @param content The content that might contain markdown code blocks
     * @return The content with markdown code block syntax removed
     */
    private String stripMarkdownCodeBlock(String content) {
        if (content.startsWith("```") && content.endsWith("```")) {
            // Find the first newline after the opening backticks
            int startIndex = content.indexOf('\n');
            if (startIndex != -1) {
                // Find the position of the closing backticks
                int endIndex = content.lastIndexOf("```");
                if (endIndex > startIndex) {
                    // Extract the content between the backticks
                    return content.substring(startIndex + 1, endIndex).trim();
                }
            }
        }
        return content;
    }

    /**
     * Process a list of messages by sending them to OpenAI's API for classification
     * and grading the compsci-technical messages
     * 
     * @param messages List of messages to process
     * @return The processed messages along with classification and grading information
     */
    public Map<String, Object> processMessages(List<Message> messages) {
        logger.info("Processing " + messages.size() + " messages");
        
        String openAIKey = environment.getProperty("openai.key");
        
        try {
            // Step 1: Classify messages as 'standard' or 'compsci-technical'
            Map<String, Object> classificationResult = classifyMessages(messages, openAIKey);
            
            // Ensure the classification was successful
            if (classificationResult.containsKey("error") || classificationResult.containsKey("rawResponse")) {
                return classificationResult; // Return early if classification failed
            }
            
            // Step 2: Extract compsci-technical message pairs for grading
            Map<String, Object> parsedContent = (Map<String, Object>) classificationResult.get("classification");
            List<Map<String, Object>> classifiedMessages = (List<Map<String, Object>>) parsedContent.get("messages");
            
            // Filter for compsci-technical messages
            List<Map<String, Object>> technicalMessages = classifiedMessages.stream()
                .filter(msg -> "compsci-technical".equals(msg.get("type")))
                .toList();
            
            if (technicalMessages.isEmpty()) {
                logger.info("No compsci-technical messages found to grade");
                Map<String, Object> result = new HashMap<>(classificationResult);
                result.put("gradingResult", Map.of("message", "No technical questions to grade"));
                return result;
            }
            
            // Step 3: Send the compsci-technical messages to the specialized model for grading
            Map<String, Object> gradingResult = gradeTechnicalMessages(technicalMessages, openAIKey);
            
            // Step 4: Combine the classification and grading results
            Map<String, Object> result = new HashMap<>(classificationResult);
            result.put("gradingResult", gradingResult);
            
            return result;
            
        } catch (Exception e) {
            logger.severe("Error processing messages: " + e.getMessage());
            e.printStackTrace();
            
            // Fallback: return just the original messages if processing fails
            Map<String, Object> result = new HashMap<>();
            result.put("originalMessages", messages);
            result.put("error", "Failed to process messages: " + e.getMessage());
            
            return result;
        }
    }
    
    /**
     * Classify messages as 'standard' or 'compsci-technical'
     * 
     * @param messages List of messages to classify
     * @param openAIKey API key for OpenAI
     * @return Classification results
     */
    private Map<String, Object> classifyMessages(List<Message> messages, String openAIKey) throws Exception {
        // Format the messages for OpenAI
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("Classify each question and answer pair as either 'standard' or 'compsci-technical'. Do not include the introductory questions. ");
        promptBuilder.append("Return a JSON array in this exact format: ");
        promptBuilder.append("{\"messages\": [{\"question\": \"...\", \"answer\": \"...\", \"type\":\"standard|compsci-technical\"}]}.\n\n");
        
        // Add the messages from the request
        promptBuilder.append("Here are the messages to classify:\n");
        for (int i = 0; i < messages.size(); i += 2) {
            String question = (i < messages.size()) ? messages.get(i).getContent() : "";
            String answer = (i + 1 < messages.size()) ? messages.get(i + 1).getContent() : "";
            
            promptBuilder.append("Question: ").append(question).append("\n");
            promptBuilder.append("Answer: ").append(answer).append("\n\n");
        }
        
        // Create the OpenAI API request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-4o-mini");
        
        Map<String, Object> messageObj = new HashMap<>();
        messageObj.put("role", "user");
        messageObj.put("content", promptBuilder.toString());
        
        requestBody.put("messages", List.of(messageObj));
        
        // Convert to JSON
        Gson gson = new Gson();
        String jsonRequestBody = gson.toJson(requestBody);
        
        // Create HTTP client and request
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + openAIKey)
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequestBody))
                .build();
        
        // Send request and get response
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        // Parse the response
        Map<String, Object> responseMap = gson.fromJson(response.body(), Map.class);
        
        // Extract the classification result from the response
        List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
        if (choices != null && !choices.isEmpty()) {
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String content = (String) message.get("content");
            
            // Try to parse the content as JSON
            try {
                // First try parsing directly
                Map<String, Object> parsedContent;
                try {
                    parsedContent = gson.fromJson(content, Map.class);
                } catch (Exception e) {
                    // If direct parsing fails, try stripping markdown formatting
                    logger.info("Direct JSON parsing failed, trying to strip markdown formatting");
                    String cleanedContent = stripMarkdownCodeBlock(content);
                    parsedContent = gson.fromJson(cleanedContent, Map.class);
                }
                
                // Return both the original messages and the classification
                Map<String, Object> result = new HashMap<>();
                result.put("originalMessages", messages);
                result.put("classification", parsedContent);
                
                return result;
            } catch (Exception e) {
                logger.warning("Failed to parse OpenAI response as JSON: " + e.getMessage());
                logger.info("Raw response content: " + content);
                
                // Return the raw content if parsing fails
                Map<String, Object> result = new HashMap<>();
                result.put("originalMessages", messages);
                result.put("rawResponse", content);
                
                return result;
            }
        }
        
        logger.warning("Unexpected response structure from OpenAI API");
        return Map.of("originalMessages", messages, "error", "Unexpected response structure from classification API");
    }
    
    /**
     * Grade compsci-technical message pairs using a specialized model
     * 
     * @param technicalMessages List of technical question-answer pairs to grade
     * @param openAIKey API key for OpenAI
     * @return Grading results
     */
    private Map<String, Object> gradeTechnicalMessages(List<Map<String, Object>> technicalMessages, String openAIKey) throws Exception {
        if (technicalMessages.isEmpty()) {
            return Map.of("message", "No technical messages to grade");
        }
        
        // Format the messages for the grading model
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("Please grade the following computer science/technical question and answer pairs. ");
        promptBuilder.append("For each answer, provide a score from 0-10, detailed feedback, and highlight any misconceptions or errors. ");
        promptBuilder.append("Return results in this JSON format: {\"graded_responses\": [{\"question\": \"...\", \"answer\": \"...\", \"score\": 0-10, \"feedback\": \"...\", \"errors\": [\"error1\", \"error2\"]}]}\n\n");
        
        // Add the technical message pairs
        promptBuilder.append("Technical questions and answers to grade:\n\n");
        for (Map<String, Object> msgPair : technicalMessages) {
            promptBuilder.append("Question: ").append(msgPair.get("question")).append("\n");
            promptBuilder.append("Answer: ").append(msgPair.get("answer")).append("\n\n");
        }
        
        // Create the OpenAI API request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "ft:gpt-4o-mini-2024-07-18:personal:cs-mini-2:B9HBRWzs"); // Using the specialized model
        
        Map<String, Object> messageObj = new HashMap<>();
        messageObj.put("role", "user");
        messageObj.put("content", promptBuilder.toString());
        
        requestBody.put("messages", List.of(messageObj));
        
        // Convert to JSON
        Gson gson = new Gson();
        String jsonRequestBody = gson.toJson(requestBody);
        
        // Create HTTP client and request
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + openAIKey)
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequestBody))
                .build();
        
        // Send request and get response
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        // Parse the response
        Map<String, Object> responseMap = gson.fromJson(response.body(), Map.class);
        
        // Extract the grading result from the response
        List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
        if (choices != null && !choices.isEmpty()) {
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String content = (String) message.get("content");
            
            // Try to parse the content as JSON
            try {
                // First try parsing directly
                Map<String, Object> parsedContent;
                try {
                    parsedContent = gson.fromJson(content, Map.class);
                    return parsedContent;
                } catch (Exception e) {
                    // If direct parsing fails, try stripping markdown formatting
                    logger.info("Direct JSON parsing failed for grading, trying to strip markdown formatting");
                    String cleanedContent = stripMarkdownCodeBlock(content);
                    parsedContent = gson.fromJson(cleanedContent, Map.class);
                    return parsedContent;
                }
            } catch (Exception e) {
                logger.warning("Failed to parse grading response as JSON: " + e.getMessage());
                logger.info("Raw grading response content: " + content);
                
                // Return the raw content if parsing fails
                return Map.of("rawGradingResponse", content);
            }
        }
        
        logger.warning("Unexpected response structure from grading API");
        return Map.of("error", "Unexpected response structure from grading API");
    }
}
