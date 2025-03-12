package org.tukma.resume.controllers;

/**
 * Controller handling resume upload and processing operations.
 * Provides endpoints for uploading resumes, checking processing status,
 * and retrieving similarity scores.
 */

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.tukma.auth.models.UserEntity;
import org.tukma.jobs.models.Job;
import org.tukma.resume.dtos.ProcessingStatusResponse;
import org.tukma.resume.dtos.ResumeUploadRequest;
import org.tukma.resume.dtos.ResumeUploadResponse;
import org.tukma.resume.dtos.SimilarityScoreResponse;
import org.tukma.resume.models.Resume;
import org.tukma.resume.services.ResumeClientService;
import org.tukma.resume.services.ResumeDataService;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;

/**
 * REST controller for resume-related operations.
 * Base path: /api/v1/resume
 */
@Controller
@Validated
@RequestMapping("/api/v1/resume")
public class ResumeController {

    private final ResumeClientService resumeClientService;
    private final ResumeDataService resumeDataService;
    private final org.tukma.jobs.services.JobService jobService;

    @Autowired
    public ResumeController(ResumeClientService resumeClientService, ResumeDataService resumeDataService, org.tukma.jobs.services.JobService jobService) {
        this.resumeClientService = resumeClientService;
        this.resumeDataService = resumeDataService;
        this.jobService = jobService;
    }

    /**
     * Uploads a resume file with associated keywords for processing.
     *
     * @param request Contains the resume file (PDF) and list of keywords for analysis
     * @return ResponseEntity containing a hash identifier for tracking the upload
     * @throws IOException if there are issues reading the resume file
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadResume(@Valid @ModelAttribute ResumeUploadRequest request)
            throws IOException {
        if (request.getKeywords() == null || request.getKeywords().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "At least one keyword is required for general resume uploads"
            ));
        }
        
        return ResponseEntity.ok(
                resumeClientService.uploadResume(request.getResume().getBytes(), request.getKeywords())
                        .block()
        );
    }

    /**
     * Uploads a resume file for a specific job application using job access key.
     * Uses keywords from the job instead of requesting them from the user.
     *
     * @param accessKey The access key of the job being applied for
     * @param request   Contains the resume file (keywords are extracted from the job)
     * @return ResponseEntity containing a hash identifier
     * @throws IOException if there are issues reading the resume file
     */
    @PostMapping(value = "/upload-for-job/{accessKey}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadResumeForJob(
            @PathVariable String accessKey,
            @ModelAttribute ResumeUploadRequest request)
            throws IOException {

        // Get current user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity currentUser = (UserEntity) auth.getPrincipal();
        Long userId = currentUser.getId();

        // Find job by access key
        Job job = jobService.getByAccessKey(accessKey);
        if (job == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Job not found with access key: " + accessKey
            ));
        }
        
        // Get keywords from the job instead of the request
        Map<String, Object> jobWithKeywords = jobService.getJobWithKeywords(job);
        List<String> keywords = (List<String>) jobWithKeywords.get("keywords");
        
        if (keywords == null || keywords.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "This job has no keywords defined for resume matching"
            ));
        }

        // Upload resume using client service with job keywords
        ResumeUploadResponse response = resumeClientService.uploadResume(
                request.getResume().getBytes(),
                keywords
        ).block();

        // Store initial entry in database with null results (will be updated later)
        if (response != null) {
            resumeDataService.saveResumeData(response.getHash(), null, job.getId(), userId);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves the similarity score for a resume and stores it in the database.
     * If the resume is associated with a job, it will update the job association.
     *
     * @param hash The unique identifier returned from the upload endpoint
     * @return ResponseEntity containing the similarity analysis results
     */
    @GetMapping("/score/{hash}")
    public ResponseEntity<?> getSimilarityScore(@PathVariable String hash) {
        // Get score from microservice
        SimilarityScoreResponse response = resumeClientService.getSimilarityScore(hash).block();

        // Try to find resume in database to associate with job
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            UserEntity currentUser = (UserEntity) auth.getPrincipal();
            Long userId = currentUser.getId();

            // Check if we have this resume stored
            Optional<Resume> resumeOpt = resumeDataService.getResumeByHash(hash);
            if (resumeOpt.isPresent() && response != null && response.getResult() != null) {
                Resume resume = resumeOpt.get();
                Job job = resume.getJob();

                // Update the resume data with results
                resumeDataService.saveResumeData(
                        hash,
                        response.getResult().toString(),
                        job.getId(),
                        userId
                );
            }
        }

        return ResponseEntity.ok(response);
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
     * Get stored resume data from the database
     *
     * @param hash The resume hash
     * @return ResponseEntity containing the resume data with parsed results
     */
    @GetMapping("/data/{hash}")
    public ResponseEntity<?> getResumeData(@PathVariable String hash) {
        Optional<Resume> resume = resumeDataService.getResumeByHash(hash);

        if (resume.isPresent()) {
            Map<String, Object> response = Map.of(
                    "resume", resume.get(),
                    "parsedResults", resumeDataService.parseResumeResults(resume.get())
            );
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get all resumes submitted for a specific job
     *
     * @param accessKey The job access key
     * @return List of resumes with their results
     */
    @GetMapping("/job/{accessKey}")
    public ResponseEntity<?> getResumesByJob(@PathVariable String accessKey) {
        // Find job by access key
        Job job = jobService.getByAccessKey(accessKey);
        if (job == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "Job not found with access key: " + accessKey
            ));
        }

        // Check if the current user is the owner of the job
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            UserEntity currentUser = (UserEntity) auth.getPrincipal();
            if (!job.getOwner().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                        "error", "You are not authorized to view resumes for this job"
                ));
            }

            List<Resume> resumes = resumeDataService.getResumesByJob(job.getId());

            // Convert resumes to a more frontend-friendly format with parsed results
            List<Map<String, Object>> formattedResumes = resumes.stream().map(resume -> {
                Map<String, Object> formatted = new HashMap<>();
                formatted.put("resume", resume);
                formatted.put("parsedResults", resumeDataService.parseResumeResults(resume));
                return formatted;
            }).collect(java.util.stream.Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "job", job,
                    "resumes", formattedResumes
            ));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "error", "You must be logged in to view resumes"
        ));
    }

    /**
     * Get all resumes submitted by the currently authenticated applicant user
     *
     * @return List of all resumes with their parsed results
     */
    @GetMapping("/my-resumes")
    public ResponseEntity<?> getMyResumes() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", "You must be logged in to view your resumes"
            ));
        }

        UserEntity currentUser = (UserEntity) auth.getPrincipal();

        // Only allow applicants to access this endpoint
        if (currentUser.isRecruiter()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "error", "This endpoint is only available for applicants"
            ));
        }

        Long userId = currentUser.getId();
        List<Resume> resumes = resumeDataService.getResumesByUser(userId);

        // Convert resumes to a more frontend-friendly format with parsed results
        List<Map<String, Object>> formattedResumes = resumes.stream().map(resume -> {
            Map<String, Object> formatted = new HashMap<>();
            formatted.put("resume", resume);
            formatted.put("parsedResults", resumeDataService.parseResumeResults(resume));
            formatted.put("job", resume.getJob());
            return formatted;
        }).collect(java.util.stream.Collectors.toList());

        return ResponseEntity.ok(Map.of(
                "resumes", formattedResumes
        ));
    }

    /**
     * Get the resume submitted by the current applicant user for a specific job
     *
     * @param accessKey The access key of the job
     * @return Resume data with parsed results
     */
    @GetMapping("/my-application/{accessKey}")
    public ResponseEntity<?> getMyApplicationForJob(@PathVariable String accessKey) {
        // Check authentication
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", "You must be logged in to view your application"
            ));
        }

        UserEntity currentUser = (UserEntity) auth.getPrincipal();

        // Only allow applicants to access this endpoint
        if (currentUser.isRecruiter()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "error", "This endpoint is only available for applicants"
            ));
        }

        // Find job by access key
        Job job = jobService.getByAccessKey(accessKey);
        if (job == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "Job not found with access key: " + accessKey
            ));
        }

        // Get resume for this user and job
        Long userId = currentUser.getId();
        Optional<Resume> resumeOpt = resumeDataService.getResumeByJobAndUser(job.getId(), userId);

        if (resumeOpt.isPresent()) {
            Resume resume = resumeOpt.get();
            Map<String, Object> response = new HashMap<>();
            response.put("resume", resume);
            response.put("parsedResults", resumeDataService.parseResumeResults(resume));
            response.put("job", job);

            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "No application found for this job"
            ));
        }
    }

    /**
     * Get a specific applicant's resume for a job (for recruiters)
     *
     * @param accessKey   The access key of the job
     * @param applicantId The ID of the applicant
     * @return Resume data with parsed results
     */
    @GetMapping("/job/{accessKey}/applicant/{applicantId}")
    public ResponseEntity<?> getApplicantResumeForJob(
            @PathVariable String accessKey,
            @PathVariable Long applicantId) {
        // Check authentication
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", "You must be logged in to view applicant resumes"
            ));
        }

        UserEntity currentUser = (UserEntity) auth.getPrincipal();

        // Only allow recruiters to access this endpoint
        if (!currentUser.isRecruiter()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "error", "This endpoint is only available for recruiters"
            ));
        }

        // Find job by access key
        Job job = jobService.getByAccessKey(accessKey);
        if (job == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "Job not found with access key: " + accessKey
            ));
        }

        // Check if the current recruiter is the owner of the job
        if (!job.getOwner().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "error", "You are not authorized to view resumes for this job"
            ));
        }

        // Get resume for the specified applicant and job
        Optional<Resume> resumeOpt = resumeDataService.getResumeByJobAndUser(job.getId(), applicantId);

        if (resumeOpt.isPresent()) {
            Resume resume = resumeOpt.get();
            Map<String, Object> response = new HashMap<>();
            response.put("resume", resume);
            response.put("parsedResults", resumeDataService.parseResumeResults(resume));
            response.put("applicant", resume.getOwner());
            response.put("job", job);

            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "No application found for this applicant and job"
            ));
        }
    }
}
