package org.tukma.interview.services;

import org.springframework.stereotype.Service;
import jakarta.annotation.PreDestroy;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

import com.google.gson.Gson;
import org.tukma.auth.models.UserEntity;
import org.tukma.interview.models.CommunicationResults;
import org.tukma.interview.models.TechnicalResults;
import org.tukma.interview.repositories.CommunicationResultsRepository;
import org.tukma.interview.repositories.TechnicalResultsRepository;

@Service
public class MessageProcessingService {

    private final Environment environment;
    private final CommunicationResultsRepository communicationResultsRepository;
    private final TechnicalResultsRepository technicalResultsRepository;
    private final org.tukma.jobs.services.JobService jobService;
    private final ExecutorService executorService;
    private static final Logger logger = Logger.getLogger(MessageProcessingService.class.getName());

    public MessageProcessingService(Environment environment,
            CommunicationResultsRepository communicationResultsRepository,
            TechnicalResultsRepository technicalResultsRepository,
            org.tukma.jobs.services.JobService jobService) {
        this.environment = environment;
        this.communicationResultsRepository = communicationResultsRepository;
        this.technicalResultsRepository = technicalResultsRepository;
        this.jobService = jobService;
        // Create a thread pool for async operations
        this.executorService = Executors.newFixedThreadPool(4);
        logger.info("Initialized executor service for async message processing");
    }

    /**
     * Clean up method that shuts down the executor service when the application is
     * closing
     */
    @PreDestroy
    public void cleanup() {
        logger.info("Shutting down executor service for async message processing");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    logger.severe("Executor service did not terminate");
                }
            }
        } catch (InterruptedException ie) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
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
     * @return The processed messages along with classification and grading
     *         information
     */
    public Map<String, Object> processMessages(List<Message> messages) {
        return processMessages(messages, null);
    }

    /**
     * Process a list of messages by sending them to OpenAI's API for classification
     * and grading the compsci-technical messages, and store the results in the
     * database
     * 
     * @param messages    List of messages to process
     * @param currentUser The current authenticated user (may be null)
     * @return The processed messages along with classification and grading
     *         information
     */
    public Map<String, Object> processMessages(List<Message> messages, UserEntity currentUser) {
        return processMessages(messages, currentUser, null);
    }

    /**
     * Process a list of messages by sending them to OpenAI's API for classification
     * and grading the compsci-technical messages, and store the results in the
     * database
     * 
     * @param messages    List of messages to process
     * @param currentUser The current authenticated user (may be null)
     * @param accessKey   The job access key (may be null)
     * @return The processed messages along with classification and grading
     *         information
     */
    public Map<String, Object> processMessages(List<Message> messages, UserEntity currentUser, String accessKey) {
        logger.info("Processing " + messages.size() + " messages");

        String openAIKey = environment.getProperty("openai.key");

        try {
            // Step 0: Start grammar correction for all answers asynchronously
            List<CompletableFuture<String>> correctionFutures = new ArrayList<>();
            List<String> correctedAnswers = new ArrayList<>();

            // For each answer message (odd indices), start a correction task
            for (int i = 1; i < messages.size(); i += 2) {
                final int index = i;
                String originalAnswer = messages.get(index).getContent();
                correctedAnswers.add(originalAnswer); // Initially use original answer as placeholder

                // Queue up grammar correction for each answer asynchronously
                CompletableFuture<String> correctionFuture = CompletableFuture.supplyAsync(() -> {
                    try {
                        return correctGrammarInAnswer(originalAnswer, openAIKey);
                    } catch (Exception e) {
                        logger.warning("Error correcting grammar for answer #" + index + ": " + e.getMessage());
                        // Fall back to original answer if correction fails
                        return originalAnswer;
                    }
                }, executorService);

                correctionFutures.add(correctionFuture);
            }

            // Update messages with corrected answers as they become available
            // This runs in parallel while we prepare and send the classification request
            CompletableFuture<Void> allCorrectionsFuture = CompletableFuture.allOf(
                    correctionFutures.toArray(new CompletableFuture[0]))
                    .thenAccept(v -> {
                        for (int i = 0; i < correctionFutures.size(); i++) {
                            try {
                                correctedAnswers.set(i, correctionFutures.get(i).get());
                            } catch (Exception e) {
                                logger.warning("Failed to get corrected answer #" + i + ": " + e.getMessage());
                                // Keep the original answer that was set as placeholder
                            }
                        }
                    });

            // Step 1: Classify messages as 'standard' or 'compsci-technical' (with original
            // answers for now)
            Map<String, Object> classificationResult = classifyMessages(messages, openAIKey);

            // Ensure the classification was successful
            if (classificationResult.containsKey("error") || classificationResult.containsKey("rawResponse")) {
                return classificationResult; // Return early if classification failed
            }

            // Wait for grammar corrections to complete before proceeding with detailed
            // analysis
            try {
                allCorrectionsFuture.get(30, TimeUnit.SECONDS); // Set a timeout to avoid blocking forever
                logger.info("All grammar corrections completed successfully");

                // Now we can update the answer messages with corrected text
                for (int i = 0, answerIndex = 0; i < messages.size(); i += 2) {
                    if (i + 1 < messages.size()) {
                        Message originalMsg = messages.get(i + 1);
                        Message correctedMsg = new Message();
                        correctedMsg.setRole(originalMsg.getRole());
                        correctedMsg.setContent(correctedAnswers.get(answerIndex++));
                        // Replace the original message with corrected version
                        messages.set(i + 1, correctedMsg);
                    }
                }
            } catch (Exception e) {
                logger.warning("Timed out or failed waiting for grammar corrections: " + e.getMessage());
                // Continue with whatever corrections were completed
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

            // Step 3: Send the compsci-technical messages to the specialized model for
            // grading (asynchronously)
            CompletableFuture<Map<String, Object>> gradingResultFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return gradeTechnicalMessages(technicalMessages, openAIKey);
                } catch (Exception e) {
                    logger.severe("Error in async technical grading: " + e.getMessage());
                    e.printStackTrace();
                    return Map.of("error", "Failed to process technical messages: " + e.getMessage());
                }
            }, executorService);

            // Step 3.5: Filter for standard messages and grade communication skills
            // (asynchronously)
            List<Map<String, Object>> standardMessages = classifiedMessages.stream()
                    .filter(msg -> "standard".equals(msg.get("type")))
                    .toList();

            CompletableFuture<Map<String, Object>> communicationResultFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    if (!standardMessages.isEmpty()) {
                        return gradeCommunicationSkills(standardMessages, openAIKey);
                    } else {
                        return Map.of("message", "No standard questions to grade communication skills");
                    }
                } catch (Exception e) {
                    logger.severe("Error in async communication grading: " + e.getMessage());
                    e.printStackTrace();
                    return Map.of("error", "Failed to process communication messages: " + e.getMessage());
                }
            }, executorService);

            // Wait for both futures to complete and get results
            Map<String, Object> gradingResult = gradingResultFuture.join();
            Map<String, Object> communicationResult = communicationResultFuture.join();

            // Step 4: Combine the classification and grading results
            Map<String, Object> result = new HashMap<>(classificationResult);
            result.put("gradingResult", gradingResult);
            result.put("communicationResult", communicationResult);

            // Store results asynchronously if we have a valid user and data
            if (currentUser != null) {
                CompletableFuture.runAsync(() -> {
                    if (communicationResult.containsKey("communication_evaluation")) {
                        storeCommunicationResults(communicationResult, currentUser, accessKey);
                    }
                }, executorService);

                CompletableFuture.runAsync(() -> {
                    if (gradingResult.containsKey("graded_responses")) {
                        storeTechnicalResults(gradingResult, currentUser, accessKey);
                    }
                }, executorService);
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
     * @param messages  List of messages to classify
     * @param openAIKey API key for OpenAI
     * @return Classification results
     */
    /**
     * Maximum number of retry attempts for API requests
     */
    private static final int MAX_RETRY_ATTEMPTS = 3;

    /**
     * Base delay in milliseconds for exponential backoff retry strategy
     */
    private static final long RETRY_BASE_DELAY_MS = 1000;

    private Map<String, Object> classifyMessages(List<Message> messages, String openAIKey) throws Exception {
        // Format the messages for OpenAI
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append(
                "Classify each question and answer pair as either 'standard' or 'compsci-technical'. Do not include the introductory questions. ");
        promptBuilder
                .append("Make sure the question is actually a question, and the answer is actually an answer.");
        promptBuilder.append(
                "Do not include the system's end interview message, which is typically the last message in the transcript. ");
        promptBuilder.append("Return a JSON array in this exact format: ");
        promptBuilder.append(
                "{\"messages\": [{\"question\": \"...\", \"answer\": \"...\", \"type\":\"standard|compsci-technical\"}]}.\n\n");

        // Add the messages from the request (with grammar-corrected answers)
        promptBuilder.append("Here are the messages to classify:\n");
        for (int i = 0; i < messages.size(); i += 2) {
            String question = (i < messages.size()) ? messages.get(i).getContent() : "";
            String answer = (i + 1 < messages.size()) ? messages.get(i + 1).getContent() : "";

            // Placeholder for the corrected answer, will be updated asynchronously
            if (i + 1 < messages.size()) {
                // We'll handle grammar correction asynchronously in a later step
                // For now we keep the original answer
            }

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

        // Send request and get response with retry capability
        HttpResponse<String> response = sendRequestWithRetry(client, request);

        // Parse the response
        Map<String, Object> responseMap = gson.fromJson(response.body(), Map.class);

        // Extract the classification result from the response
        List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
        if (choices != null && !choices.isEmpty()) {
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String content = (String) message.get("content");

            // Try to parse the content as JSON with retry
            int attempts = 0;
            Exception lastException = null;

            while (attempts < MAX_RETRY_ATTEMPTS) {
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
                    lastException = e;
                    logger.warning("Failed to parse JSON on attempt " + (attempts + 1) + ": " + e.getMessage());

                    // If we've exhausted retries, break out of the loop
                    if (++attempts >= MAX_RETRY_ATTEMPTS) {
                        break;
                    }

                    // Retry the API request to get a properly formatted response
                    try {
                        long delayMs = RETRY_BASE_DELAY_MS * (long) Math.pow(2, attempts - 1);
                        logger.info("Retrying API request in " + delayMs + "ms...");
                        Thread.sleep(delayMs);

                        HttpResponse<String> retryResponse = sendRequestWithRetry(client, request);
                        responseMap = gson.fromJson(retryResponse.body(), Map.class);
                        choices = (List<Map<String, Object>>) responseMap.get("choices");

                        if (choices != null && !choices.isEmpty()) {
                            message = (Map<String, Object>) choices.get(0).get("message");
                            content = (String) message.get("content");
                            logger.info("Received new response to parse on retry #" + attempts);
                        } else {
                            logger.warning("Received invalid response structure on retry");
                        }
                    } catch (Exception retryEx) {
                        logger.warning("Error during retry: " + retryEx.getMessage());
                    }
                }
            }

            // If we get here, all retry attempts failed
            logger.warning("All JSON parsing attempts failed. Last error: "
                    + (lastException != null ? lastException.getMessage() : "Unknown"));
            logger.info("Raw response content: " + content);

            // Return the raw content if parsing fails after all retries
            Map<String, Object> result = new HashMap<>();
            result.put("originalMessages", messages);
            result.put("rawResponse", content);

            return result;
        }

        logger.warning("Unexpected response structure from OpenAI API");
        return Map.of("originalMessages", messages, "error", "Unexpected response structure from classification API");
    }

    /**
     * Grade compsci-technical message pairs using a specialized model
     * 
     * @param technicalMessages List of technical question-answer pairs to grade
     * @param openAIKey         API key for OpenAI
     * @return Grading results
     */
    /**
     * Grade communication skills based on standard messages
     * 
     * @param standardMessages List of standard question-answer pairs to evaluate
     * @param openAIKey        API key for OpenAI
     * @return Communication skills assessment results
     */
    private Map<String, Object> gradeCommunicationSkills(List<Map<String, Object>> standardMessages, String openAIKey)
            throws Exception {
        if (standardMessages.isEmpty()) {
            return Map.of("message", "No standard messages to grade");
        }

        // Format the messages for the grading model
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append(
                "Please analyze the following interview question-answer pairs to evaluate communication skills. ");
        promptBuilder.append("For each evaluation metric, provide a score and explanation. ");
        promptBuilder.append(
                "Ensure that the overall_score is explicitly on a scale of 1-10, where 1 is poor and 10 is excellent. ");
        promptBuilder.append("The metrics are: \n");
        promptBuilder.append("* **Question-response relevance**: Score 1-5 how directly answers address questions\n");
        promptBuilder.append("* **Information density**: Ratio of substantive content words to total words\n");
        promptBuilder.append("* **Topic maintenance**: Track percentage of statements that stay on relevant topics\n");
        promptBuilder.append("* **Specificity ratio**: Count of specific examples/details vs. generic statements\n");
        promptBuilder.append("* **Clarification frequency**: Number of times asking for or providing clarification\n");
        promptBuilder.append(
                "* **Active vs. passive voice**: Percentage of sentences in active voice (higher typically better)\n");
        promptBuilder.append("Return the evaluation in JSON format with the following structure: ");
        promptBuilder.append(
                "{\"communication_evaluation\": {\"metrics\": {\"question_response_relevance\": {\"score\": N, \"explanation\": \"...\"},");
        promptBuilder.append(" \"information_density\": {\"ratio\": N.N, \"explanation\": \"...\"}}, ");
        promptBuilder.append(
                "\"overall_score\": N.N (must be on a scale of 1-10), \"strengths\": [\"...\"], \"areas_for_improvement\": [\"...\"]}}\n\n");

        // Add the standard message pairs
        promptBuilder.append("Standard questions and answers to evaluate:\n\n");
        for (Map<String, Object> msgPair : standardMessages) {
            String question = (String) msgPair.get("question");
            String answer = (String) msgPair.get("answer");

            // The answers have already been grammar-corrected in the classification step

            promptBuilder.append("Question: ").append(question).append("\n");
            promptBuilder.append("Answer: ").append(answer).append("\n\n");
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

        // Send request and get response with retry capability
        HttpResponse<String> response = sendRequestWithRetry(client, request);

        // Parse the response
        Map<String, Object> responseMap = gson.fromJson(response.body(), Map.class);

        // Extract the communication evaluation result from the response
        List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
        if (choices != null && !choices.isEmpty()) {
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String content = (String) message.get("content");

            // Try to parse the content as JSON with retry mechanism
            int attempts = 0;
            Exception lastException = null;

            while (attempts < MAX_RETRY_ATTEMPTS) {
                try {
                    // First try parsing directly
                    Map<String, Object> parsedContent;
                    try {
                        parsedContent = gson.fromJson(content, Map.class);
                        return parsedContent;
                    } catch (Exception e) {
                        // If direct parsing fails, try stripping markdown formatting
                        logger.info(
                                "Direct JSON parsing failed for communication skills, trying to strip markdown formatting");
                        String cleanedContent = stripMarkdownCodeBlock(content);
                        parsedContent = gson.fromJson(cleanedContent, Map.class);
                        return parsedContent;
                    }
                } catch (Exception e) {
                    lastException = e;
                    logger.warning("Failed to parse communication skills JSON on attempt " + (attempts + 1) + ": "
                            + e.getMessage());

                    // If we've exhausted retries, break out of the loop
                    if (++attempts >= MAX_RETRY_ATTEMPTS) {
                        break;
                    }

                    // Retry the API request to get a properly formatted response
                    try {
                        long delayMs = RETRY_BASE_DELAY_MS * (long) Math.pow(2, attempts - 1);
                        logger.info("Retrying communication skills API request in " + delayMs + "ms...");
                        Thread.sleep(delayMs);

                        HttpResponse<String> retryResponse = sendRequestWithRetry(client, request);
                        responseMap = gson.fromJson(retryResponse.body(), Map.class);
                        choices = (List<Map<String, Object>>) responseMap.get("choices");

                        if (choices != null && !choices.isEmpty()) {
                            message = (Map<String, Object>) choices.get(0).get("message");
                            content = (String) message.get("content");
                            logger.info("Received new communication skills response to parse on retry #" + attempts);
                        } else {
                            logger.warning("Received invalid communication skills response structure on retry");
                        }
                    } catch (Exception retryEx) {
                        logger.warning("Error during communication skills retry: " + retryEx.getMessage());
                    }
                }
            }

            // If we get here, all retry attempts failed
            logger.warning("All communication skills JSON parsing attempts failed. Last error: " +
                    (lastException != null ? lastException.getMessage() : "Unknown"));
            logger.info("Raw communication skills response content: " + content);

            // Return the raw content if parsing fails after all retries
            return Map.of("rawCommunicationResponse", content);
        }

        logger.warning("Unexpected response structure from communication skills evaluation API");
        return Map.of("error", "Unexpected response structure from communication skills evaluation API");
    }

    private Map<String, Object> gradeTechnicalMessages(List<Map<String, Object>> technicalMessages, String openAIKey)
            throws Exception {
        if (technicalMessages.isEmpty()) {
            return Map.of("message", "No technical messages to grade");
        }

        // Format the messages for the grading model
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append(
                " You are an essay grader assistant for technical coding in a technical interview, give the rating based on technical accuracy and communication efficiency.");
        promptBuilder.append(
                "In here, you are required to grade the answer following a specific schema, with a score from 0 to 100, where 0 is the worst and 100 is perfect.");
        promptBuilder.append(
                "Make full use of the 0-100 range. Grades should not be afraid to use any number in this range, including floats and numbers NOT divisible by 5 for the sake of granularity");
        promptBuilder.append("Please grade the following computer science/technical question and answer pairs. ");
        promptBuilder.append(
                "Return results in this JSON format: {\"graded_responses\": [{\"question\": \"...\", \"answer\": \"...\", \"score\": 0-100, \"feedback\": \"...\", \"errors\": [\"error1\", \"error2\"]}]}\n\n");

        // Add the technical message pairs
        promptBuilder.append("Technical questions and answers to grade:\n\n");
        for (Map<String, Object> msgPair : technicalMessages) {
            String question = (String) msgPair.get("question");
            String answer = (String) msgPair.get("answer");

            // The answers have already been grammar-corrected in the classification step

            promptBuilder.append("Question: ").append(question).append("\n");
            promptBuilder.append("Answer: ").append(answer).append("\n\n");
        }

        // Create the OpenAI API request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-4o");

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

        // Send request and get response with retry capability
        HttpResponse<String> response = sendRequestWithRetry(client, request);

        // Parse the response
        Map<String, Object> responseMap = gson.fromJson(response.body(), Map.class);

        // Extract the grading result from the response
        List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
        if (choices != null && !choices.isEmpty()) {
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String content = (String) message.get("content");

            // Try to parse the content as JSON with retry mechanism
            int attempts = 0;
            Exception lastException = null;

            while (attempts < MAX_RETRY_ATTEMPTS) {
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
                    lastException = e;
                    logger.warning("Failed to parse grading JSON on attempt " + (attempts + 1) + ": " + e.getMessage());

                    // If we've exhausted retries, break out of the loop
                    if (++attempts >= MAX_RETRY_ATTEMPTS) {
                        break;
                    }

                    // Retry the API request to get a properly formatted response
                    try {
                        long delayMs = RETRY_BASE_DELAY_MS * (long) Math.pow(2, attempts - 1);
                        logger.info("Retrying grading API request in " + delayMs + "ms...");
                        Thread.sleep(delayMs);

                        HttpResponse<String> retryResponse = sendRequestWithRetry(client, request);
                        responseMap = gson.fromJson(retryResponse.body(), Map.class);
                        choices = (List<Map<String, Object>>) responseMap.get("choices");

                        if (choices != null && !choices.isEmpty()) {
                            message = (Map<String, Object>) choices.get(0).get("message");
                            content = (String) message.get("content");
                            logger.info("Received new grading response to parse on retry #" + attempts);
                        } else {
                            logger.warning("Received invalid grading response structure on retry");
                        }
                    } catch (Exception retryEx) {
                        logger.warning("Error during grading retry: " + retryEx.getMessage());
                    }
                }
            }

            // If we get here, all retry attempts failed
            logger.warning("All grading JSON parsing attempts failed. Last error: " +
                    (lastException != null ? lastException.getMessage() : "Unknown"));
            logger.info("Raw grading response content: " + content);

            // Return the raw content if parsing fails after all retries
            return Map.of("rawGradingResponse", content);
        }

        logger.warning("Unexpected response structure from grading API");
        return Map.of("error", "Unexpected response structure from grading API");
    }

    /**
     * Correct grammar and spelling in an answer using OpenAI
     * 
     * @param answer    The original answer text with potential grammar issues
     * @param openAIKey API key for OpenAI
     * @return The corrected answer text
     */
    private String correctGrammarInAnswer(String answer, String openAIKey) {
        if (answer == null || answer.trim().isEmpty()) {
            return answer;
        }

        try {
            // Format the prompt for grammar correction
            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append("Do not correct the grammar and spelling in the following interview answer.");
            promptBuilder.append("They are not important as much as being able to deliver quality.");
            promptBuilder.append("Because the text in the input is not from text input mediums but rather through transcripts, be lenient in cases where it might sometimes make sense, as it is possible that the transcription module misheard the details");
            promptBuilder.append("Return only the corrected text");
            promptBuilder
                    .append("If you think the user meant to say a technical term, correct it to the correct technical term.");
            promptBuilder
                    .append("Give the full answer, do not use ellipsis.");
            promptBuilder.append("Answer to correct: ").append(answer);

            // Create the OpenAI API request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-4o-mini"); // Using a smaller model for grammar correction

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

            // Send request and get response with retry capability
            HttpResponse<String> response = sendRequestWithRetry(client, request);

            // Parse the response
            Map<String, Object> responseMap = gson.fromJson(response.body(), Map.class);

            // Extract the corrected text from the response
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                String content = (String) message.get("content");

                // Return the corrected content
                logger.info("Grammar correction applied to answer");
                return content;
            }

            logger.warning("Unexpected response structure from grammar correction API");
            return answer; // Return the original answer if correction fails

        } catch (Exception e) {
            logger.warning("Error during grammar correction: " + e.getMessage());
            return answer; // Return the original answer if an error occurs
        }
    }

    /**
     * Store communication results in the database.
     * Extracts the overall score, strengths, and areas for improvement from the
     * evaluation results.
     * 
     * @param communicationResult The raw communication evaluation result from the
     *                            API
     * @param user                The user entity to associate with these results
     * @param accessKey           The job access key (may be null)
     */
    /**
     * Store technical grading results in the database.
     * Processes each graded technical question and creates individual
     * TechnicalResults records.
     * 
     * @param gradingResult The raw technical grading result from the API
     * @param user          The user entity to associate with these results
     * @param accessKey     The job access key (may be null)
     */
    private void storeTechnicalResults(Map<String, Object> gradingResult, UserEntity user, String accessKey) {
        try {
            // Extract the graded responses array
            List<Map<String, Object>> gradedResponses = (List<Map<String, Object>>) gradingResult
                    .get("graded_responses");
            if (gradedResponses == null || gradedResponses.isEmpty()) {
                logger.warning("Cannot store technical results: missing or empty graded_responses data");
                return;
            }

            // Get the job if accessKey is provided
            org.tukma.jobs.models.Job job = null;
            if (accessKey != null && !accessKey.isEmpty()) {
                job = jobService.getByAccessKey(accessKey);
                if (job == null) {
                    logger.warning("Could not find job for accessKey: " + accessKey);
                    // Continue without setting the job reference
                }
            }

            // Delete any existing technical results for this user and access key
            if (accessKey != null && !accessKey.isEmpty()) {
                List<TechnicalResults> existingResults = technicalResultsRepository
                        .findByUser_IdAndAccessKeyOrderByCreatedAtDesc(user.getId(), accessKey);

                if (!existingResults.isEmpty()) {
                    logger.info("Deleting " + existingResults.size() + " existing technical results for user "
                            + user.getUsername() + " and accessKey " + accessKey);
                    technicalResultsRepository.deleteAll(existingResults);
                }
            }

            // Process each graded question-answer pair
            for (Map<String, Object> gradedResponse : gradedResponses) {
                String question = (String) gradedResponse.get("question");
                String answer = (String) gradedResponse.get("answer");

                // Extract the score
                Integer score = null;
                if (gradedResponse.containsKey("score")) {
                    Object scoreObj = gradedResponse.get("score");
                    if (scoreObj instanceof Number) {
                        score = ((Number) scoreObj).intValue();

                        // Ensure score is in the 0-100 range
                        if (score > 100) {
                            score = 100; // Cap at 100
                        } else if (score < 0) {
                            score = 0; // Minimum of 0
                        }
                    }
                }

                if (score == null) {
                    logger.warning("Cannot store technical result: missing or invalid score for question: " + question);
                    continue; // Skip this question and move to the next
                }

                // Extract feedback
                String feedback = (String) gradedResponse.get("feedback");

                // Extract errors as a concatenated string
                String combinedErrors = null;
                if (gradedResponse.containsKey("errors")) {
                    Object errorsObj = gradedResponse.get("errors");
                    if (errorsObj instanceof List) {
                        List<String> errors = (List<String>) errorsObj;
                        if (!errors.isEmpty()) {
                            StringJoiner joiner = new StringJoiner(". ");
                            for (String error : errors) {
                                joiner.add(error);
                            }
                            combinedErrors = joiner.toString();
                            if (!combinedErrors.endsWith(".")) {
                                combinedErrors = combinedErrors + ".";
                            }
                        }
                    } else if (errorsObj instanceof String) {
                        combinedErrors = (String) errorsObj;
                    }
                }

                // Create and save the technical results entity
                TechnicalResults results = new TechnicalResults();
                results.setUser(user);
                results.setQuestionText(question);
                results.setAnswerText(answer);
                results.setScore(score);
                results.setFeedback(feedback);
                results.setErrors(combinedErrors);

                // Set the accessKey and job if provided
                if (accessKey != null && !accessKey.isEmpty()) {
                    results.setAccessKey(accessKey);
                    results.setJob(job); // This may be null if the job wasn't found
                }

                technicalResultsRepository.save(results);
            }

            logger.info("Stored " + gradedResponses.size() + " technical results for user " + user.getUsername());

        } catch (Exception e) {
            logger.severe("Error storing technical results: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Helper method to send HTTP requests with retry capability.
     * Uses exponential backoff for retries.
     * 
     * @param client  The HTTP client
     * @param request The HTTP request to send
     * @return The HTTP response
     * @throws Exception If all retry attempts fail
     */
    private HttpResponse<String> sendRequestWithRetry(HttpClient client, HttpRequest request) throws Exception {
        Exception lastException = null;

        for (int attempt = 0; attempt < MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                if (attempt > 0) {
                    logger.info("Sending HTTP request, attempt " + (attempt + 1) + " of " + MAX_RETRY_ATTEMPTS);
                }

                return client.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (Exception e) {
                lastException = e;
                logger.warning("HTTP request failed on attempt " + (attempt + 1) + ": " + e.getMessage());

                // If this is the last attempt, don't sleep, just throw
                if (attempt >= MAX_RETRY_ATTEMPTS - 1) {
                    break;
                }

                // Exponential backoff
                long delayMs = RETRY_BASE_DELAY_MS * (long) Math.pow(2, attempt);
                logger.info("Retrying HTTP request in " + delayMs + "ms...");
                Thread.sleep(delayMs);
            }
        }

        // If we get here, all attempts failed
        throw new Exception("Failed to send HTTP request after " + MAX_RETRY_ATTEMPTS + " attempts. Last error: " +
                (lastException != null ? lastException.getMessage() : "Unknown"));
    }

    private void storeCommunicationResults(Map<String, Object> communicationResult, UserEntity user, String accessKey) {
        try {
            Map<String, Object> evaluation = (Map<String, Object>) communicationResult.get("communication_evaluation");
            if (evaluation == null) {
                logger.warning("Cannot store communication results: missing communication_evaluation data");
                return;
            }

            // Delete any existing communication results for this user and access key
            if (accessKey != null && !accessKey.isEmpty()) {
                List<CommunicationResults> existingResults = communicationResultsRepository
                        .findByUser_IdAndAccessKeyOrderByCreatedAtDesc(user.getId(), accessKey);

                if (!existingResults.isEmpty()) {
                    logger.info("Deleting " + existingResults.size() + " existing communication results for user "
                            + user.getUsername() + " and accessKey " + accessKey);
                    communicationResultsRepository.deleteAll(existingResults);
                }
            }

            // Extract overall score
            Double overallScore = null;
            if (evaluation.containsKey("overall_score")) {
                Object scoreObj = evaluation.get("overall_score");
                if (scoreObj instanceof Number) {
                    overallScore = ((Number) scoreObj).doubleValue();

                    // Ensure score is in the 1-10 range
                    if (overallScore > 10) {
                        // If score is above 10, normalize it to a 0-10 scale assuming it's a 0-100
                        // scale
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

            // Set the accessKey if provided
            if (accessKey != null && !accessKey.isEmpty()) {
                results.setAccessKey(accessKey);

                // Try to fetch the job using accessKey and set it if found
                org.tukma.jobs.models.Job job = jobService.getByAccessKey(accessKey);
                if (job != null) {
                    results.setJob(job);
                } else {
                    logger.warning("Could not find job for accessKey: " + accessKey);
                    // Continue without setting the job reference
                }
            }

            communicationResultsRepository.save(results);
            logger.info("Stored communication results for user " + user.getUsername());

        } catch (Exception e) {
            logger.severe("Error storing communication results: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
