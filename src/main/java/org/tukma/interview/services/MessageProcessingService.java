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
import java.util.StringJoiner;
import java.util.ArrayList;

import com.google.gson.Gson;
import org.tukma.auth.models.UserEntity;
import org.tukma.interview.models.CommunicationResults;
import org.tukma.interview.repositories.CommunicationResultsRepository;

@Service
public class MessageProcessingService {

    private final Environment environment;
    private final CommunicationResultsRepository communicationResultsRepository;
    private static final Logger logger = Logger.getLogger(MessageProcessingService.class.getName());
    
    public MessageProcessingService(Environment environment, CommunicationResultsRepository communicationResultsRepository) {
        this.environment = environment;
        this.communicationResultsRepository = communicationResultsRepository;
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
        return processMessages(messages, null);
    }
    
    /**
     * Process a list of messages by sending them to OpenAI's API for classification
     * and grading the compsci-technical messages, and store the results in the database
     * 
     * @param messages List of messages to process
     * @param currentUser The current authenticated user (may be null)
     * @return The processed messages along with classification and grading information
     */
    public Map<String, Object> processMessages(List<Message> messages, UserEntity currentUser) {
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
            
            // Step 3.5: Filter for standard messages and grade communication skills
            List<Map<String, Object>> standardMessages = classifiedMessages.stream()
                .filter(msg -> "standard".equals(msg.get("type")))
                .toList();
                
            Map<String, Object> communicationResult = new HashMap<>();
            if (!standardMessages.isEmpty()) {
                communicationResult = gradeCommunicationSkills(standardMessages, openAIKey);
            } else {
                communicationResult.put("message", "No standard questions to grade communication skills");
            }
            
            // Step 4: Combine the classification and grading results
            Map<String, Object> result = new HashMap<>(classificationResult);
            result.put("gradingResult", gradingResult);
            result.put("communicationResult", communicationResult);
            
            // Store communication results if we have a valid user and communication data
            if (currentUser != null && communicationResult.containsKey("communication_evaluation")) {
                storeCommunicationResults(communicationResult, currentUser);
            }
            
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
    /**
     * Grade communication skills based on standard messages
     * 
     * @param standardMessages List of standard question-answer pairs to evaluate
     * @param openAIKey API key for OpenAI
     * @return Communication skills assessment results
     */
    private Map<String, Object> gradeCommunicationSkills(List<Map<String, Object>> standardMessages, String openAIKey) throws Exception {
        if (standardMessages.isEmpty()) {
            return Map.of("message", "No standard messages to grade");
        }
        
        // Format the messages for the grading model
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("Please analyze the following interview question-answer pairs to evaluate communication skills. ");
        promptBuilder.append("For each evaluation metric, provide a score and explanation. ");
        promptBuilder.append("Ensure that the overall_score is explicitly on a scale of 1-10, where 1 is poor and 10 is excellent. ");
        promptBuilder.append("The metrics are: \n");
        promptBuilder.append("* **Question-response relevance**: Score 1-5 how directly answers address questions\n");
        promptBuilder.append("* **Information density**: Ratio of substantive content words to total words\n");
        promptBuilder.append("* **Topic maintenance**: Track percentage of statements that stay on relevant topics\n");
        promptBuilder.append("* **Specificity ratio**: Count of specific examples/details vs. generic statements\n");
        promptBuilder.append("* **Clarification frequency**: Number of times asking for or providing clarification\n");
        promptBuilder.append("* **Active vs. passive voice**: Percentage of sentences in active voice (higher typically better)\n");
        promptBuilder.append("Return the evaluation in JSON format with the following structure: ");
        promptBuilder.append("{\"communication_evaluation\": {\"metrics\": {\"question_response_relevance\": {\"score\": N, \"explanation\": \"...\"},");
        promptBuilder.append(" \"information_density\": {\"ratio\": N.N, \"explanation\": \"...\"}}, ");
        promptBuilder.append("\"overall_score\": N.N (must be on a scale of 1-10), \"strengths\": [\"...\"], \"areas_for_improvement\": [\"...\"]}}\n\n");
        
        // Add the standard message pairs
        promptBuilder.append("Standard questions and answers to evaluate:\n\n");
        for (Map<String, Object> msgPair : standardMessages) {
            promptBuilder.append("Question: ").append(msgPair.get("question")).append("\n");
            promptBuilder.append("Answer: ").append(msgPair.get("answer")).append("\n\n");
        }
        
        // Create the OpenAI API request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-4o-mini"); // Using a general-purpose model for communication assessment
        
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
        
        // Extract the communication evaluation result from the response
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
                    logger.info("Direct JSON parsing failed for communication skills, trying to strip markdown formatting");
                    String cleanedContent = stripMarkdownCodeBlock(content);
                    parsedContent = gson.fromJson(cleanedContent, Map.class);
                    return parsedContent;
                }
            } catch (Exception e) {
                logger.warning("Failed to parse communication skills response as JSON: " + e.getMessage());
                logger.info("Raw communication skills response content: " + content);
                
                // Return the raw content if parsing fails
                return Map.of("rawCommunicationResponse", content);
            }
        }
        
        logger.warning("Unexpected response structure from communication skills evaluation API");
        return Map.of("error", "Unexpected response structure from communication skills evaluation API");
    }

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
    
    /**
     * Store communication results in the database.
     * Extracts the overall score, strengths, and areas for improvement from the evaluation results.
     * 
     * @param communicationResult The raw communication evaluation result from the API
     * @param user The user entity to associate with these results
     */
    private void storeCommunicationResults(Map<String, Object> communicationResult, UserEntity user) {
        try {
            Map<String, Object> evaluation = (Map<String, Object>) communicationResult.get("communication_evaluation");
            if (evaluation == null) {
                logger.warning("Cannot store communication results: missing communication_evaluation data");
                return;
            }
            
            // Extract overall score
            Double overallScore = null;
            if (evaluation.containsKey("overall_score")) {
                Object scoreObj = evaluation.get("overall_score");
                if (scoreObj instanceof Number) {
                    overallScore = ((Number) scoreObj).doubleValue();
                    
                    // Ensure score is in the 1-10 range
                    if (overallScore > 10) {
                        // If score is above 10, normalize it to a 0-10 scale assuming it's a 0-100 scale
                        overallScore = overallScore / 10.0;
                        // Ensure it doesn't exceed 10 due to rounding
                        overallScore = Math.min(10.0, overallScore);
                    } else if (overallScore < 1) {
                        // If score is below 1, set to 1 (minimum score)
                        overallScore = 1.0;
                    }
                    
                    // Round to 2 decimal places for consistency
                    overallScore = Math.round(overallScore * 100.0) / 100.0;
                }
            }
            
            if (overallScore == null) {
                logger.warning("Cannot store communication results: missing or invalid overall_score");
                return;
            }
            
            // Extract strengths as a single concatenated string
            String combinedStrengths = null;
            if (evaluation.containsKey("strengths")) {
                Object strengthsObj = evaluation.get("strengths");
                if (strengthsObj instanceof List) {
                    List<String> strengths = (List<String>) strengthsObj;
                    if (!strengths.isEmpty()) {
                        StringJoiner joiner = new StringJoiner(". ");
                        for (String strength : strengths) {
                            joiner.add(strength);
                        }
                        combinedStrengths = joiner.toString();
                        if (!combinedStrengths.endsWith(".")) {
                            combinedStrengths = combinedStrengths + ".";
                        }
                    }
                } else if (strengthsObj instanceof String) {
                    combinedStrengths = (String) strengthsObj;
                }
            }
            
            // Extract areas for improvement as a single concatenated string
            String combinedImprovements = null;
            if (evaluation.containsKey("areas_for_improvement")) {
                Object improvementsObj = evaluation.get("areas_for_improvement");
                if (improvementsObj instanceof List) {
                    List<String> improvements = (List<String>) improvementsObj;
                    if (!improvements.isEmpty()) {
                        StringJoiner joiner = new StringJoiner(". ");
                        for (String improvement : improvements) {
                            joiner.add(improvement);
                        }
                        combinedImprovements = joiner.toString();
                        if (!combinedImprovements.endsWith(".")) {
                            combinedImprovements = combinedImprovements + ".";
                        }
                    }
                } else if (improvementsObj instanceof String) {
                    combinedImprovements = (String) improvementsObj;
                }
            }
            
            // Create and save the communication results entity
            CommunicationResults results = new CommunicationResults();
            results.setUser(user);
            results.setOverallScore(overallScore);
            results.setStrengths(combinedStrengths);
            results.setAreasForImprovement(combinedImprovements);
            
            communicationResultsRepository.save(results);
            logger.info("Stored communication results for user " + user.getUsername());
            
        } catch (Exception e) {
            logger.severe("Error storing communication results: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
