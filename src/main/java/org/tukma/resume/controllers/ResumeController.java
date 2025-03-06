package org.tukma.resume.controllers;

/**
 * Controller handling resume upload and processing operations.
 * Provides endpoints for uploading resumes, checking processing status,
 * and retrieving similarity scores.
 */

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.tukma.auth.models.UserEntity;
import org.tukma.resume.dtos.ProcessingStatusResponse;
import org.tukma.resume.dtos.ResumeUploadRequest;
import org.tukma.resume.dtos.ResumeUploadResponse;
import org.tukma.resume.dtos.SimilarityScoreResponse;
import org.tukma.resume.models.Resume;
import org.tukma.resume.services.ResumeClientService;
import org.tukma.resume.services.ResumeService;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST controller for resume-related operations.
 * Base path: /api/v1/resume
 */
@Controller
@Validated
@RequestMapping("/api/v1/resume")
public class ResumeController {

    private final ResumeClientService resumeClientService;
    private final ResumeService resumeService;

    @Autowired
    public ResumeController(ResumeClientService resumeClientService, ResumeService resumeService) {
        this.resumeClientService = resumeClientService;
        this.resumeService = resumeService;
    }

    /**
     * Uploads a resume file with associated keywords for processing.
     *
     * @param request Contains the resume file (PDF) and list of keywords for analysis
     * @param accessKey The access key for the job this resume is for
     * @return ResponseEntity containing a hash identifier for tracking the upload
     * @throws IOException if there are issues reading the resume file
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResumeUploadResponse> uploadResume(
            @Valid @ModelAttribute ResumeUploadRequest request,
            @RequestParam String accessKey) throws IOException {
        // Get the authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UserEntity currentUser = (UserEntity) auth.getPrincipal();
        
        // Upload the resume to the external service
        ResumeUploadResponse response = resumeClientService.uploadResume(
                request.getResume().getBytes(), request.getKeywords()).block();
        
        // Create a placeholder entry in our database
        if (response != null && response.getHash() != null) {
            resumeService.saveResumeResultByAccessKey(response.getHash(), null, accessKey, currentUser);
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves the similarity score for a previously uploaded resume.
     *
     * @param hash The unique identifier returned from the upload endpoint
     * @return ResponseEntity containing the similarity analysis results
     */
    @GetMapping("/score/{hash}")
    public ResponseEntity<SimilarityScoreResponse> getSimilarityScore(@PathVariable String hash) {
        // Get the authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UserEntity currentUser = (UserEntity) auth.getPrincipal();
        
        // Check if we already have the result stored
        Resume storedResume = resumeService.getResumeByHash(hash).orElse(null);
        
        if (storedResume != null && storedResume.getResult() != null) {
            // We already have the result locally
            SimilarityScoreResponse response = new SimilarityScoreResponse();
            response.setHash(hash);
            response.setResult(storedResume.getResult());
            return ResponseEntity.ok(response);
        }
        
        // If not, fetch from external service and store
        if (storedResume != null) {
            // Get the job accessKey from the resume
            String jobAccessKey = storedResume.getJob().getAccessKey();
            return ResponseEntity.ok(
                    resumeService.fetchAndStoreResultsByAccessKey(hash, jobAccessKey, currentUser)
            );
        } else {
            // If we don't have any record of this hash, just fetch without storing
            return ResponseEntity.ok(
                    resumeClientService.getSimilarityScore(hash).block()
            );
        }
    }

    /**
     * Checks the processing status of a resume upload.
     *
     * @param hash The unique identifier returned from the upload endpoint
     * @return ResponseEntity containing the current processing status
     */
    @GetMapping("/status/{hash}")
    public ResponseEntity<ProcessingStatusResponse> checkStatus(@PathVariable String hash) {
        return ResponseEntity.ok(
                resumeClientService.checkProcessingStatus(hash)
                        .block()
        );
    }
    
    /**
     * Get all resumes for a specific job.
     *
     * @param jobId The ID of the job
     * @return List of resumes associated with the job
     */
    @GetMapping("/job/{jobId}")
    public ResponseEntity<?> getResumesByJobId(@PathVariable Long jobId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        UserEntity currentUser = (UserEntity) auth.getPrincipal();
        List<Resume> resumes = resumeService.getResumesByJobId(jobId);
        
        return ResponseEntity.ok(resumes);
    }
    
    /**
     * Get all resumes for a specific job using its access key.
     *
     * @param accessKey The access key of the job
     * @return List of resumes associated with the job
     */
    @GetMapping("/job/key/{accessKey}")
    public ResponseEntity<?> getResumesByJobAccessKey(@PathVariable String accessKey) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        List<Resume> resumes = resumeService.getResumesByJobAccessKey(accessKey);
        
        return ResponseEntity.ok(resumes);
    }
    
    /**
     * Get all resumes uploaded by the current user.
     *
     * @return List of resumes uploaded by the user
     */
    @GetMapping("/my-resumes")
    public ResponseEntity<?> getMyResumes() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        UserEntity currentUser = (UserEntity) auth.getPrincipal();
        List<Resume> resumes = resumeService.getResumesByOwnerId(currentUser.getId());
        
        return ResponseEntity.ok(resumes);
    }
    
    /**
     * Get detailed resume analysis results by hash.
     *
     * @param hash The hash identifying the resume
     * @return Detailed result information or error if not found
     */
    @GetMapping("/details/{hash}")
    public ResponseEntity<?> getResumeDetails(@PathVariable String hash) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        Optional<Resume> resumeOpt = resumeService.getResumeByHash(hash);
        if (resumeOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Resume not found for hash: " + hash));
        }
        
        Resume resume = resumeOpt.get();
        
        // If we don't have results yet, try to fetch them
        if (resume.getResult() == null || resume.getResult().isEmpty()) {
            UserEntity currentUser = (UserEntity) auth.getPrincipal();
            String jobAccessKey = resume.getJob().getAccessKey();
            SimilarityScoreResponse response = resumeService.fetchAndStoreResultsByAccessKey(
                    hash, jobAccessKey, currentUser);
            
            if (response != null && response.getResult() != null) {
                // Refresh the resume to get the updated results
                resume = resumeService.getResumeByHash(hash).orElse(resume);
            }
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("id", resume.getId());
        response.put("resumeHash", resume.getResumeHash());
        response.put("result", resume.getResult());
        response.put("jobId", resume.getJob().getId());
        response.put("jobTitle", resume.getJob().getTitle());
        response.put("createdAt", resume.getCreatedAt());
        
        return ResponseEntity.ok(response);
    }
}
