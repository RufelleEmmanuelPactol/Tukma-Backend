package org.tukma.resume.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tukma.auth.models.UserEntity;
import org.tukma.jobs.models.Job;
import org.tukma.jobs.services.JobService;
import org.tukma.resume.dtos.SimilarityScoreResponse;
import org.tukma.resume.models.Resume;
import org.tukma.resume.repositories.ResumeRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * Service for managing resume data and operations.
 */
@Service
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final JobService jobService;
    private final ResumeClientService resumeClientService;
    private final Gson gson = new Gson();

    @Autowired
    public ResumeService(ResumeRepository resumeRepository, JobService jobService, ResumeClientService resumeClientService) {
        this.resumeRepository = resumeRepository;
        this.jobService = jobService;
        this.resumeClientService = resumeClientService;
    }

    /**
     * Create or update a resume entry with analysis results using job access key.
     *
     * @param resumeHash The hash identifying the resume
     * @param resultStr The analysis result JSON string or null for initial creation
     * @param accessKey The access key of the job this resume is for
     * @param owner The user who uploaded the resume
     * @return The saved Resume entity
     */
    public Resume saveResumeResultByAccessKey(String resumeHash, String resultStr, String accessKey, UserEntity owner) {
        // Find the job by access key
        Job job = jobService.getByAccessKey(accessKey);
        if (job == null) {
            throw new IllegalArgumentException("Job not found with access key: " + accessKey);
        }
        
        // Use the existing method with the job ID
        return saveResumeResult(resumeHash, resultStr, job.getId(), owner);
    }

    /**
     * Create or update a resume entry with analysis results.
     *
     * @param resumeHash The hash identifying the resume
     * @param resultStr The analysis result JSON string or null for initial creation
     * @param jobId The ID of the job this resume is for
     * @param owner The user who uploaded the resume
     * @return The saved Resume entity
     */
    public Resume saveResumeResult(String resumeHash, String resultStr, Long jobId, UserEntity owner) {
        Optional<Resume> existingResume = resumeRepository.findByResumeHash(resumeHash);
        
        // Parse the result string to a Map if provided
        Map<String, Object> resultMap = null;
        if (resultStr != null) {
            try {
                // Try to parse as a Map (JSON object)
                resultMap = gson.fromJson(resultStr, Map.class);
            } catch (JsonSyntaxException e) {
                // If parsing fails, store as a raw string
                resultMap = new HashMap<>();
                resultMap.put("raw", resultStr);
            }
        }
        
        if (existingResume.isPresent()) {
            // Update existing resume
            Resume resume = existingResume.get();
            if (resultMap != null) {
                resume.setResult(resultMap);
            }
            return resumeRepository.save(resume);
        } else {
            // Create new resume entry
            Job job = jobService.getJobById(jobId);
            if (job == null) {
                throw new IllegalArgumentException("Job not found with ID: " + jobId);
            }
            
            Resume resume = new Resume();
            resume.setResumeHash(resumeHash);
            resume.setResult(resultMap); // May be null for initial creation
            resume.setJob(job);
            resume.setOwner(owner);
            
            return resumeRepository.save(resume);
        }
    }

    /**
     * Get resume analysis results by hash.
     *
     * @param resumeHash The hash identifying the resume
     * @return Optional containing the Resume if found
     */
    public Optional<Resume> getResumeByHash(String resumeHash) {
        return resumeRepository.findByResumeHash(resumeHash);
    }

    /**
     * Find all resumes for a specific job.
     *
     * @param jobId The job ID
     * @return List of resumes for the job
     */
    public List<Resume> getResumesByJobId(Long jobId) {
        return resumeRepository.findByJob_Id(jobId);
    }
    
    /**
     * Find all resumes for a specific job by access key.
     *
     * @param accessKey The job access key
     * @return List of resumes for the job
     */
    public List<Resume> getResumesByJobAccessKey(String accessKey) {
        Job job = jobService.getByAccessKey(accessKey);
        if (job == null) {
            return List.of();
        }
        return resumeRepository.findByJob_Id(job.getId());
    }

    /**
     * Find all resumes uploaded by a specific user.
     *
     * @param ownerId The user ID
     * @return List of resumes uploaded by the user
     */
    public List<Resume> getResumesByOwnerId(Long ownerId) {
        return resumeRepository.findByOwner_Id(ownerId);
    }

    /**
     * Retrieve and store similarity scores for a resume.
     * This method fetches results from the external service and saves them to the database.
     *
     * @param resumeHash The hash identifying the resume
     * @param jobId The ID of the job this resume is for
     * @param owner The user who uploaded the resume
     * @return The similarity score response
     */
    public SimilarityScoreResponse fetchAndStoreResults(String resumeHash, Long jobId, UserEntity owner) {
        SimilarityScoreResponse response = resumeClientService.getSimilarityScore(resumeHash).block();
        
        if (response != null && response.getResult() != null) {
            // Convert result to string if it's not already
            String resultStr;
            if (response.getResult() instanceof String) {
                resultStr = (String) response.getResult();
            } else if (response.getResult() instanceof Map) {
                // If it's already a Map, convert to JSON string
                resultStr = gson.toJson(response.getResult());
            } else {
                // For any other type, use toString
                resultStr = response.getResult().toString();
            }
            
            // Save to database
            saveResumeResult(resumeHash, resultStr, jobId, owner);
        }
        
        return response;
    }
    
    /**
     * Retrieve and store similarity scores for a resume using job access key.
     *
     * @param resumeHash The hash identifying the resume
     * @param accessKey The access key of the job
     * @param owner The user who uploaded the resume
     * @return The similarity score response
     */
    public SimilarityScoreResponse fetchAndStoreResultsByAccessKey(String resumeHash, String accessKey, UserEntity owner) {
        Job job = jobService.getByAccessKey(accessKey);
        if (job == null) {
            throw new IllegalArgumentException("Job not found with access key: " + accessKey);
        }
        
        return fetchAndStoreResults(resumeHash, job.getId(), owner);
    }
}
