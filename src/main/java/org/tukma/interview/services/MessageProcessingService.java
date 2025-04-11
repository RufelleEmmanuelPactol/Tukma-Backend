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
     * Process a list of messages by sending them to OpenAI's API for classification
     * 
     * @param messages List of messages to process
     * @return The processed messages along with OpenAI's classification response
     */
    public Map<String, Object> processMessages(List<Message> messages) {
        logger.info("Processing " + messages.size() + " messages");
        
        String openAIKey = environment.getProperty("openai.key");
        
        try {
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
                    Map<String, Object> parsedContent = gson.fromJson(content, Map.class);
                    
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
            
        } catch (Exception e) {
            logger.severe("Error calling OpenAI API: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Fallback: return just the original messages if the API call fails
        Map<String, Object> result = new HashMap<>();
        result.put("originalMessages", messages);
        result.put("error", "Failed to process messages through OpenAI API");
        
        return result;
    }
}
