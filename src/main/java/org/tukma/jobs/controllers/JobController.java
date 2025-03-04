package org.tukma.jobs.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.tukma.auth.models.UserEntity;
import org.tukma.jobs.dtos.JobCreateRequest;
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

    @GetMapping("/get-jobs")
    public ResponseEntity<List<Map<String, Object>>> getAllJobs() {
        UserEntity currentUser = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        List<Map<String, Object>> jobsWithKeywords = jobService.getJobsWithKeywords(currentUser);
        return ResponseEntity.ok(jobsWithKeywords);
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


}