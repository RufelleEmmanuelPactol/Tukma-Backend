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

import java.util.List;
import java.util.Map;

@Controller
@Validated
@RequestMapping("/api/v1/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping("/create-job")
    public ResponseEntity<Job> createJob(@RequestBody @Validated JobCreateRequest request) {
        Job job = jobService.createJob((UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal(),
                request.getTitle(),
                request.getDescription());
        return ResponseEntity.ok(job);
    }

    @GetMapping("/get-jobs")
    public ResponseEntity<List<Job>> getAllJobs() {
        List<Job> jobs = jobService.getJobByOwner((UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        return ResponseEntity.ok(jobs);
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
        return ResponseEntity.ok(job);
    }

    @PostMapping("/upload-application/{accessKey}")
    public ResponseEntity<?> uploadResume(@PathVariable String accessKey) {
        return null;
    }


}