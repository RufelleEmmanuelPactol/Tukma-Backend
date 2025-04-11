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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/v1/interview")
public class InterviewController {

    private static final Logger logger = Logger.getLogger(InterviewController.class.getName());
    
    private final MessageProcessingService messageProcessingService;
    private final CommunicationResultsRepository communicationResultsRepository;
    private final TechnicalResultsRepository technicalResultsRepository;
    private final JobService jobService;
    
    @Autowired
    public InterviewController(MessageProcessingService messageProcessingService,
                              CommunicationResultsRepository communicationResultsRepository,
                              TechnicalResultsRepository technicalResultsRepository,
                              JobService jobService) {
        this.messageProcessingService = messageProcessingService;
        this.communicationResultsRepository = communicationResultsRepository;
        this.technicalResultsRepository = technicalResultsRepository;
        this.jobService = jobService;
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
     * This is useful for recruiters to see all communication evaluations for a specific job
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
        
        // Verify user is the job owner (only recruiters/job owners should see all results)
        if (!job.getOwner().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You are not authorized to view these communication results"));
        }
        
        // Get all communication results for this job
        List<CommunicationResults> results = communicationResultsRepository.findByAccessKeyOrderByCreatedAtDesc(accessKey);
        
        // Return the results with the job information
        Map<String, Object> response = new HashMap<>();
        response.put("job", job);
        response.put("communicationResults", results);
        response.put("count", results.size());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get a specific user's communication result for a job
     * This can be used by both recruiters (to see a specific applicant) and applicants (to see their own result)
     * 
     * @param accessKey The job's access key
     * @param userId The user's ID
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
     * This is useful for recruiters to see all technical evaluations for a specific job
     * 
     * @param accessKey The job's access key
     * @return List of technical results for the job
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
        
        // Verify user is the job owner (only recruiters/job owners should see all results)
        if (!job.getOwner().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You are not authorized to view these technical results"));
        }
        
        // Get all technical results for this job
        List<TechnicalResults> results = technicalResultsRepository.findByAccessKeyOrderByCreatedAtDesc(accessKey);
        
        // Return the results with the job information
        Map<String, Object> response = new HashMap<>();
        response.put("job", job);
        response.put("technicalResults", results);
        response.put("count", results.size());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get a specific user's technical results for a job
     * This can be used by both recruiters (to see a specific applicant) and applicants (to see their own results)
     * 
     * @param accessKey The job's access key
     * @param userId The user's ID
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
        
        // Return all results for this user and job
        return ResponseEntity.ok(results);
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
}
