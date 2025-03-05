package org.tukma.jobs.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.tukma.auth.models.UserEntity;
import org.tukma.jobs.dtos.JobCreateRequest;
import org.tukma.jobs.dtos.JobEditRequest;
import org.tukma.jobs.dtos.PagedJobsResponse;
import org.tukma.jobs.models.Job;
import org.tukma.jobs.services.JobService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.tukma.jobs.models.Keyword;
import org.tukma.jobs.repositories.KeywordRepository;

@Controller
@Validated
@RequestMapping("/api/v1/jobs")
public class JobController {

    private final JobService jobService;
    private final KeywordRepository keywordRepository;

    public JobController(JobService jobService, KeywordRepository keywordRepository) {
        this.jobService = jobService;
        this.keywordRepository = keywordRepository;
    }

    @PostMapping("/create-job")
    public ResponseEntity<Job> createJob(@RequestBody @Validated JobCreateRequest request) {
        UserEntity currentUser = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Job job = jobService.createJob(currentUser, request);
        return ResponseEntity.ok(job);
    }

    /**
     * Get all jobs (deprecated - use the paginated endpoint instead)
     */
    @GetMapping("/get-jobs")
    public ResponseEntity<List<Map<String, Object>>> getAllJobs() {
        UserEntity currentUser = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        List<Map<String, Object>> jobsWithKeywords = jobService.getJobsWithKeywords(currentUser);
        return ResponseEntity.ok(jobsWithKeywords);
    }
    
    /**
     * Get paginated jobs, sorted by updatedAt in descending order (most recent first)
     * 
     * @param page The page number (0-based, defaults to 0)
     * @param size The page size (defaults to 10)
     * @return Paginated response containing jobs and pagination metadata
     */
    @GetMapping("/get-jobs-owner")
    public ResponseEntity<PagedJobsResponse> getPaginatedJobs(
            @RequestParam(defaultValue = "0") int page, 
            @RequestParam(defaultValue = "10") int size) {
        UserEntity currentUser = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        PagedJobsResponse pagedResponse = jobService.getPagedJobsWithKeywords(currentUser, page, size);
        return ResponseEntity.ok(pagedResponse);
    }

    @DeleteMapping("/delete-job/{accessKey}")
    public ResponseEntity<Void> deleteJob(@PathVariable String accessKey) {
        jobService.deleteJob(jobService.getByAccessKey(accessKey));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/get-job-details/{accessKey}")
    public ResponseEntity<?> getJobDetail(@PathVariable String accessKey) {
        var job = jobService.getByAccessKey(accessKey);
        if (job == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of("message", "Cannot find job with access key: " + accessKey + "."));
        }
        
        Map<String, Object> jobWithKeywords = jobService.getJobWithKeywords(job);
        return ResponseEntity.ok(jobWithKeywords);
    }

    @PostMapping("/upload-application/{accessKey}")
    public ResponseEntity<?> uploadResume(@PathVariable String accessKey) {
        return null;
    }
    
    @GetMapping("/job-metadata")
    public ResponseEntity<Map<String, Object>> getJobMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("jobTypes", Arrays.asList(Job.JobType.values()));
        metadata.put("shiftTypes", Arrays.asList(Job.ShiftType.values()));
        return ResponseEntity.ok(metadata);
    }
    
    /**
     * Endpoint to edit an existing job
     *
     * @param accessKey Unique identifier for the job
     * @param request JobEditRequest containing updated information
     * @return Updated job with keywords
     */
    @PutMapping("/edit-job/{accessKey}")
    public ResponseEntity<?> editJob(@PathVariable String accessKey, @RequestBody @Validated JobEditRequest request) {
        UserEntity currentUser = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Job job = jobService.getByAccessKey(accessKey);
        
        if (job == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of("message", "Cannot find job with access key: " + accessKey + "."));
        }
        
        try {
            Job updatedJob = jobService.updateJob(job, request, currentUser);
            return ResponseEntity.ok(jobService.getJobWithKeywords(updatedJob));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("message", "An error occurred while updating the job: " + e.getMessage()));
        }
    }

    /**
     * Get all jobs with pagination for applicants, sorted by updatedAt (most recent first)
     * This endpoint is intended for job applicants to browse available jobs
     * 
     * @param page The page number (0-based, defaults to 0)
     * @param size The number of items per page (defaults to 10)
     * @return Paginated response containing jobs and pagination metadata
     */
    @GetMapping("/get-all-jobs")
    public ResponseEntity<PagedJobsResponse> getAllJobsPaginated(
            @RequestParam(defaultValue = "0") int page, 
            @RequestParam(defaultValue = "10") int size) {
        PagedJobsResponse pagedResponse = jobService.getPagedJobsForApplicants(page, size);
        return ResponseEntity.ok(pagedResponse);
    }

}