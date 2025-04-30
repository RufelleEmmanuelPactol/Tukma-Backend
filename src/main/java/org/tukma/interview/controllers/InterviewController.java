package org.tukma.interview.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tukma.auth.models.UserEntity;
import org.tukma.interview.dtos.MessageRequest;
import org.tukma.interview.dtos.MessageResponse;
import org.tukma.interview.models.CommunicationResults;
import org.tukma.interview.models.TechnicalResults;
import org.tukma.interview.repositories.CommunicationResultsRepository;
import org.tukma.interview.repositories.TechnicalResultsRepository;
import org.tukma.interview.services.MessageProcessingService;
import org.tukma.jobs.models.Job;
import org.tukma.jobs.services.JobService;
import org.tukma.survey.repositories.AnswerRepository;
import org.tukma.auth.repositories.UserRepository;
import org.springframework.web.util.UriComponentsBuilder;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.tukma.interview.dtos.FlaskMessage;
import org.tukma.interview.dtos.FlaskApiResponse;

@RestController
@RequestMapping("/api/v1/interview")
public class InterviewController {

    private static final Logger logger = Logger.getLogger(InterviewController.class.getName());

    @Value("${flask.api.baseurl}")
    private String flaskApiBaseUrl;

    private final MessageProcessingService messageProcessingService;
    private final CommunicationResultsRepository communicationResultsRepository;
    private final TechnicalResultsRepository technicalResultsRepository;
    private final JobService jobService;
    private final AnswerRepository answerRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient;

    @Autowired
    public InterviewController(MessageProcessingService messageProcessingService,
            CommunicationResultsRepository communicationResultsRepository,
            TechnicalResultsRepository technicalResultsRepository,
            JobService jobService,
            AnswerRepository answerRepository,
            UserRepository userRepository,
            ObjectMapper objectMapper) {
        this.messageProcessingService = messageProcessingService;
        this.communicationResultsRepository = communicationResultsRepository;
        this.technicalResultsRepository = technicalResultsRepository;
        this.jobService = jobService;
        this.answerRepository = answerRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
    }

    @PostMapping("/messages")
    public ResponseEntity<?> processMessages(@RequestBody MessageRequest messageRequest) {
        // Get current authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity currentUser = null;
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserEntity) {
            currentUser = (UserEntity) auth.getPrincipal();
        }

        // Log the received messages
        logger.info("Received " + messageRequest.getMessages().size() + " messages from "
                + (currentUser != null ? currentUser.getUsername() : "unauthenticated user"));

        // Process the messages using the service and get the classification response
        Map<String, Object> processedResult = messageProcessingService.processMessages(
                messageRequest.getMessages(),
                currentUser,
                messageRequest.getAccessKey());

        // Return the processed result
        return ResponseEntity.ok(processedResult);
    }

    @PostMapping("/messages/{accessKey}")
    public ResponseEntity<?> processMessagesForJob(@RequestBody MessageRequest messageRequest,
            @PathVariable String accessKey) {
        // Override the accessKey in the request with the one from the path
        messageRequest.setAccessKey(accessKey);
        return processMessages(messageRequest);
    }

    /**
     * Get communication results for a specific job access key
     * This is useful for recruiters to see all communication evaluations for a
     * specific job
     * 
     * @param accessKey The job's access key
     * @return List of communication results for the job
     */
    @GetMapping("/communication-results/job/{accessKey}")
    public ResponseEntity<?> getCommunicationResultsByJobAccessKey(@PathVariable String accessKey) {
        // Verify job exists
        Job job = jobService.getByAccessKey(accessKey);
        if (job == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Job not found with access key: " + accessKey));
        }

        // Get current authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserEntity)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "You must be logged in to view communication results"));
        }

        UserEntity currentUser = (UserEntity) auth.getPrincipal();

        // Verify user is the job owner (only recruiters/job owners should see all
        // results)
        if (!job.getOwner().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You are not authorized to view these communication results"));
        }

        // Get all communication results for this job
        List<CommunicationResults> results = communicationResultsRepository
                .findByAccessKeyOrderByCreatedAtDesc(accessKey);

        // Return the results with the job information
        Map<String, Object> response = new HashMap<>();
        response.put("job", job);
        response.put("communicationResults", results);
        response.put("count", results.size());

        return ResponseEntity.ok(response);
    }

    /**
     * Get a specific user's communication result for a job
     * This can be used by both recruiters (to see a specific applicant) and
     * applicants (to see their own result)
     * 
     * @param accessKey The job's access key
     * @param userId    The user's ID
     * @return The user's communication result for the job
     */
    @GetMapping("/communication-results/job/{accessKey}/user/{userId}")
    public ResponseEntity<?> getUserCommunicationResultForJob(
            @PathVariable String accessKey,
            @PathVariable Long userId) {

        // Verify job exists
        Job job = jobService.getByAccessKey(accessKey);
        if (job == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Job not found with access key: " + accessKey));
        }

        // Get current authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserEntity)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "You must be logged in to view communication results"));
        }

        UserEntity currentUser = (UserEntity) auth.getPrincipal();

        // Security check: Only allow if the user is either:
        // 1. The job owner (recruiter) viewing any applicant's results, or
        // 2. The applicant viewing their own results
        boolean isJobOwner = job.getOwner().getId().equals(currentUser.getId());
        boolean isViewingOwnResults = currentUser.getId().equals(userId);

        if (!isJobOwner && !isViewingOwnResults) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You are not authorized to view these communication results"));
        }

        // Get the user's communication results for this job
        List<CommunicationResults> results = communicationResultsRepository
                .findByUser_IdAndAccessKeyOrderByCreatedAtDesc(userId, accessKey);

        if (results.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "No communication results found for this user and job"));
        }

        // Return the most recent result
        return ResponseEntity.ok(results.get(0));
    }

    /**
     * Get the current user's communication results for a specific job
     * This is a convenience endpoint for applicants to see their own results
     * 
     * @param accessKey The job's access key
     * @return The user's communication result for the job
     */
    @GetMapping("/communication-results/my/{accessKey}")
    public ResponseEntity<?> getMyResultsForJob(@PathVariable String accessKey) {
        // Get current authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserEntity)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "You must be logged in to view your communication results"));
        }

        UserEntity currentUser = (UserEntity) auth.getPrincipal();

        // Use the existing endpoint to get the results
        return getUserCommunicationResultForJob(accessKey, currentUser.getId());
    }

    /**
     * Get technical results for a specific job access key
     * This is useful for recruiters to see all technical evaluations for a specific
     * job
     * Applicants can also see aggregated results (without individual details of
     * other applicants)
     * 
     * @param accessKey The job's access key
     * @return List of technical results for the job or aggregated stats for
     *         applicants
     */
    @GetMapping("/technical-results/job/{accessKey}")
    public ResponseEntity<?> getTechnicalResultsByJobAccessKey(@PathVariable String accessKey) {
        // Verify job exists
        Job job = jobService.getByAccessKey(accessKey);
        if (job == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Job not found with access key: " + accessKey));
        }

        // Get current authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserEntity)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "You must be logged in to view technical results"));
        }

        UserEntity currentUser = (UserEntity) auth.getPrincipal();

        // Check if user is the job owner
        boolean isJobOwner = job.getOwner().getId().equals(currentUser.getId());

        // If user is not the job owner, they can only see aggregated results
        if (!isJobOwner) {
            // For applicants, show their own results with some aggregated stats
            // First, check if they have results for this job
            List<TechnicalResults> userResults = technicalResultsRepository
                    .findByUser_IdAndAccessKeyOrderByCreatedAtDesc(currentUser.getId(), accessKey);

            if (userResults.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "No technical results found for you in this job"));
            }

            // Get aggregate stats
            Double overallAverage = technicalResultsRepository.findAverageScoreByAccessKey(accessKey);
            if (overallAverage == null) {
                overallAverage = 0.0;
            } else {
                overallAverage = Math.round(overallAverage * 100.0) / 100.0;
            }

            // Calculate user's average
            Double userAverage = technicalResultsRepository.findAverageScoreByUser_IdAndAccessKey(
                    currentUser.getId(), accessKey);
            if (userAverage == null) {
                userAverage = 0.0;
            } else {
                userAverage = Math.round(userAverage * 100.0) / 100.0;
            }

            // Return the results for the applicant
            Map<String, Object> response = new HashMap<>();
            response.put("job", job);
            response.put("technicalResults", userResults);
            response.put("userScore", userAverage);
            response.put("averageScore", overallAverage);
            response.put("isOwner", false);

            return ResponseEntity.ok(response);
        }

        // Get all technical results for this job
        List<TechnicalResults> results = technicalResultsRepository.findByAccessKeyOrderByCreatedAtDesc(accessKey);

        // Calculate the overall score using the repository method
        Double overallScore = technicalResultsRepository.findAverageScoreByAccessKey(accessKey);
        // Handle null result and round to 2 decimal places
        if (overallScore == null) {
            overallScore = 0.0;
        } else {
            overallScore = Math.round(overallScore * 100.0) / 100.0;
        }

        // Return the results with the job information and overall score
        Map<String, Object> response = new HashMap<>();
        response.put("job", job);
        response.put("technicalResults", results);
        response.put("count", results.size());
        response.put("overallScore", overallScore);
        response.put("isOwner", true);

        return ResponseEntity.ok(response);
    }

    /**
     * Get a specific user's technical results for a job
     * This can be used by both recruiters (to see a specific applicant) and
     * applicants (to see their own results)
     * 
     * @param accessKey The job's access key
     * @param userId    The user's ID
     * @return The user's technical results for the job
     */
    @GetMapping("/technical-results/job/{accessKey}/user/{userId}")
    public ResponseEntity<?> getUserTechnicalResultsForJob(
            @PathVariable String accessKey,
            @PathVariable Long userId) {

        // Verify job exists
        Job job = jobService.getByAccessKey(accessKey);
        if (job == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Job not found with access key: " + accessKey));
        }

        // Get current authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserEntity)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "You must be logged in to view technical results"));
        }

        UserEntity currentUser = (UserEntity) auth.getPrincipal();

        // Security check: Only allow if the user is either:
        // 1. The job owner (recruiter) viewing any applicant's results, or
        // 2. The applicant viewing their own results
        boolean isJobOwner = job.getOwner().getId().equals(currentUser.getId());
        boolean isViewingOwnResults = currentUser.getId().equals(userId);

        if (!isJobOwner && !isViewingOwnResults) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You are not authorized to view these technical results"));
        }

        // Get the user's technical results for this job
        List<TechnicalResults> results = technicalResultsRepository
                .findByUser_IdAndAccessKeyOrderByCreatedAtDesc(userId, accessKey);

        if (results.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "No technical results found for this user and job"));
        }

        // Calculate the overall score using the repository method
        Double overallScore = technicalResultsRepository.findAverageScoreByUser_IdAndAccessKey(userId, accessKey);
        // Handle null result and round to 2 decimal places
        if (overallScore == null) {
            overallScore = 0.0;
        } else {
            overallScore = Math.round(overallScore * 100.0) / 100.0;
        }

        // Create a response map with results and overall score
        Map<String, Object> response = new HashMap<>();
        response.put("technicalResults", results);
        response.put("count", results.size());
        response.put("overallScore", overallScore);
        response.put("job", job);
        response.put("isOwner", isJobOwner);

        // Return all results for this user and job with the overall score
        return ResponseEntity.ok(response);
    }

    /**
     * Get the current user's technical results for a specific job
     * This is a convenience endpoint for applicants to see their own results
     * 
     * @param accessKey The job's access key
     * @return The user's technical results for the job
     */
    @GetMapping("/technical-results/my/{accessKey}")
    public ResponseEntity<?> getMyTechnicalResultsForJob(@PathVariable String accessKey) {
        // Get current authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserEntity)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "You must be logged in to view your technical results"));
        }

        UserEntity currentUser = (UserEntity) auth.getPrincipal();

        // Use the existing endpoint to get the results
        return getUserTechnicalResultsForJob(accessKey, currentUser.getId());
    }

    /**
     * Admin endpoint to regenerate communication and technical results for users
     * based on their interview history for a specific access key.
     * THIS IS AN ADMIN ENDPOINT AND SHOULD BE SECURED PROPERLY IN PRODUCTION.
     * 
     * @return A summary of the regeneration process.
     */
    @PostMapping("/admin/regenerate-results")
    public ResponseEntity<?> regenerateResults() {
        logger.info("Starting admin regeneration of interview results for accessKey: f1m-zj7q");
        String accessKey = "f1m-zj7q";

        List<Long> userIds = answerRepository.findDistinctUserIds();
        int successCount = 0;
        int errorCount = 0;
        Map<Long, String> errors = new HashMap<>();

        logger.info("Found " + userIds.size() + " distinct user IDs in survey answers.");

        for (Long userId : userIds) {
            try {
                Optional<UserEntity> userOpt = userRepository.findById(userId);
                if (userOpt.isEmpty()) {
                    logger.warning("User with ID " + userId + " not found. Skipping regeneration.");
                    errorCount++;
                    errors.put(userId, "User not found");
                    continue;
                }
                UserEntity user = userOpt.get();
                logger.info("Processing user: " + user.getUsername() + " (ID: " + userId + ")");

                // Fetch messages from Flask API
                String firstName = user.getFirstName();
                String lastName = user.getLastName();
                String name = (firstName != null ? firstName : "") + (lastName != null ? " " + lastName : "");
                name = name.trim();
                String email = user.getUsername();

                if (name.isEmpty() || email == null) {
                    logger.warning("User " + user.getUsername() + " is missing name or email. Skipping.");
                    errorCount++;
                    errors.put(userId, "User missing name or email");
                    continue;
                }

                String urlString = UriComponentsBuilder.fromHttpUrl(flaskApiBaseUrl)
                        .pathSegment("get_messages", accessKey, name, email)
                        .toUriString();

                logger.info("Fetching messages from URL: " + urlString);

                // Build the HttpRequest
                HttpRequest flaskRequest = HttpRequest.newBuilder()
                        .uri(URI.create(urlString))
                        .GET() // Specify GET method
                        .build();

                HttpResponse<String> flaskResponse;
                try {
                    // Send request using HttpClient
                    flaskResponse = httpClient.send(flaskRequest, HttpResponse.BodyHandlers.ofString());

                } catch (IOException | InterruptedException e) {
                    logger.severe("Error fetching messages for user " + user.getUsername() + " from Flask API: "
                            + e.getMessage());
                    errorCount++;
                    errors.put(userId, "Flask API connection error: " + e.getMessage());
                    if (e instanceof InterruptedException) {
                        Thread.currentThread().interrupt(); // Restore interrupted status
                    }
                    continue;
                }

                // Check response status code
                int statusCode = flaskResponse.statusCode();
                String responseBody = flaskResponse.body();

                if (statusCode == 404) {
                    logger.warning("No interview history found (404) for user " + user.getUsername() + " at URL: "
                            + urlString);
                    errorCount++;
                    errors.put(userId, "No interview history found in Flask API (404)");
                    continue; // Skip this user if no history found
                }

                if (statusCode < 200 || statusCode >= 300 || responseBody == null) {
                    logger.warning(
                            "Failed to fetch messages for user " + user.getUsername() + ". Status: " + statusCode);
                    errorCount++;
                    errors.put(userId, "Flask API returned status: " + statusCode);
                    continue;
                }

                // Parse the response body
                FlaskApiResponse apiResponse = objectMapper.readValue(responseBody, FlaskApiResponse.class);
                List<FlaskMessage> flaskMessages = apiResponse.getMessages();

                if (flaskMessages == null || flaskMessages.isEmpty()) {
                    logger.info("No messages found for user " + user.getUsername() + " in the response.");
                } else {
                    logger.info("Retrieved " + flaskMessages.size() + " messages for user " + user.getUsername());
                }

                // Map Flask messages to our Message model
                List<org.tukma.interview.models.Message> messages = (flaskMessages == null) ? List.of()
                        : flaskMessages.stream()
                                .map(fm -> {
                                    org.tukma.interview.models.Message msg = new org.tukma.interview.models.Message();
                                    msg.setRole(fm.getRole());
                                    msg.setContent(fm.getContent());
                                    return msg;
                                }).collect(Collectors.toList());

                // Process messages using the service
                logger.info("Calling messageProcessingService for user " + user.getUsername());
                messageProcessingService.processMessages(messages, user, accessKey);

                logger.info("Successfully regenerated results for user: " + user.getUsername());
                successCount++;

            } catch (Exception e) {
                logger.severe("Unexpected error processing user ID " + userId + ": " + e.getMessage());
                e.printStackTrace();
                errorCount++;
                errors.put(userId, "Internal server error: " + e.getMessage());
            }
        }

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("message", "Regeneration process finished.");
        responseBody.put("totalUsersAttempted", userIds.size());
        responseBody.put("successfulRegenerations", successCount);
        responseBody.put("failedRegenerations", errorCount);
        responseBody.put("errors", errors);

        logger.info("Admin regeneration finished. Success: " + successCount + ", Failed: " + errorCount);

        return ResponseEntity.ok(responseBody);
    }
}
