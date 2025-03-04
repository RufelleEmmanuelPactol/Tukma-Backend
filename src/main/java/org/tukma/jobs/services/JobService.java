package org.tukma.jobs.services;

import org.springframework.stereotype.Service;
import org.tukma.auth.models.UserEntity;
import org.tukma.jobs.dtos.JobCreateRequest;
import org.tukma.jobs.dtos.JobEditRequest;
import org.tukma.jobs.models.Job;
import org.tukma.jobs.models.Keyword;
import org.tukma.jobs.repositories.JobRepository;
import org.tukma.jobs.repositories.KeywordRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class JobService {

    private JobRepository jobRepository;
    private KeywordRepository keywordRepository;


    public JobService(JobRepository jobRepository, KeywordRepository keywordRepository) {
        this.jobRepository = jobRepository;
        this.keywordRepository = keywordRepository;
    }

    public Job createJob(UserEntity jobOwner, JobCreateRequest request) {
        Job job = new Job();
        job.setDescription(request.getDescription());
        job.setOwner(jobOwner);
        job.setTitle(request.getTitle());
        job.setAddress(request.getAddress());
        job.setAccessKey(jobAccessKeyGenerator());

        // Set the new fields
        job.setType(request.getType());
        job.setShiftType(request.getShiftType());
        job.setShiftLengthHours(request.getShiftLengthHours());

        // Save the job to generate an ID before adding keywords
        jobRepository.save(job);
        
        // Add keywords if provided
        if(request.getKeywords() != null && !request.getKeywords().isEmpty()) {
            addKeywordsToJob(request.getKeywords(), job);
        }
        
        return job;
    }

    public Job getByAccessKey(String accessKey) {
        var job = jobRepository.findByAccessKey(accessKey);
        return job.orElse(null);
    }


    public List<String> addKeywordsToJob(List<String> keywords, Job job) {
        List<Keyword> currentKeywords = keywordRepository.findByKeywordOwner_Id(job.getId());
        LinkedList<String> currentKeywordsTranslation = new LinkedList<>();
        currentKeywords.forEach((x) -> currentKeywordsTranslation.addLast(x.getKeywordName()));
        LinkedList<String> successfulAdditions = new LinkedList<>();

        for (var keyword : keywords) {
            if (currentKeywordsTranslation.contains(keyword)) {
                continue;
            }
            Keyword keywordInstance = new Keyword();
            keywordInstance.setKeywordOwner(job);
            keywordInstance.setKeywordName(keyword);
            keywordRepository.save(keywordInstance);
            successfulAdditions.addLast(keyword);
        }
        return successfulAdditions;
    }


    public List<String> removeKeywordsToJob(List<String> keywords, Job job) {
        List<Keyword> currentKeywords = keywordRepository.findByKeywordOwner_Id(job.getId());
        LinkedList<String> successfulDeletes = new LinkedList<>();

        for (var keyword : keywords) {
            for (var possibleKeyword : currentKeywords) {
                if (keyword.equals(possibleKeyword.getKeywordName())) {
                    keywordRepository.delete(possibleKeyword);
                    successfulDeletes.addLast(keyword);
                    break;
                }
            }
        }

        return successfulDeletes;


    }

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private String generateSubsequence(int length) {
        StringBuilder randomString = new StringBuilder(length);
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(CHARACTERS.length());
            randomString.append(CHARACTERS.charAt(randomIndex));

        }
        return randomString.toString();
    }

    private String jobAccessKeyGenerator() {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(generateSubsequence(3));
        keyBuilder.append("-");
        keyBuilder.append(generateSubsequence(4));
        String current = keyBuilder.toString();
        if (jobRepository.existsByAccessKey(current)) {
            return jobAccessKeyGenerator();
        }
        return current;

    }

    public void deleteJob(Job job) {
        jobRepository.deleteById(job.getId());
    }

    public void deleteJob(Long id) {
        jobRepository.deleteById(id);
    }


    public List<Job> getJobByOwner(UserEntity entity) {
        return jobRepository.findByOwner_Id(entity.getId());
    }

    /**
     * Get all jobs with their associated keywords for a specific user
     *
     * @param user The user entity whose jobs should be fetched
     * @return List of maps containing job and its keywords
     */
    public List<Map<String, Object>> getJobsWithKeywords(UserEntity user) {
        List<Job> jobs = getJobByOwner(user);
        List<Map<String, Object>> jobsWithKeywords = new ArrayList<>();
        
        for (Job job : jobs) {
            jobsWithKeywords.add(getJobWithKeywords(job));
        }
        
        return jobsWithKeywords;
    }
    
    /**
     * Get a single job with its associated keywords
     *
     * @param job The job entity
     * @return Map containing job and its keywords
     */
    public Map<String, Object> getJobWithKeywords(Job job) {
        Map<String, Object> jobMap = new HashMap<>();
        jobMap.put("job", job);
        
        List<Keyword> keywords = keywordRepository.findByKeywordOwner(job);
        List<String> keywordStrings = keywords.stream().map(Keyword::getKeywordName).toList();
        jobMap.put("keywords", keywordStrings);
        
        return jobMap;
    }
    
    /**
     * Updates an existing job with new information
     *
     * @param job The job entity to update
     * @param request The request containing updated job information
     * @param currentUser The user requesting the update (for ownership verification)
     * @return Updated job entity
     * @throws IllegalArgumentException if user is not authorized to edit the job
     */
    public Job updateJob(Job job, JobEditRequest request, UserEntity currentUser) {
        // Verify ownership
        if (!job.getOwner().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("You are not authorized to edit this job.");
        }
        
        // Update job details
        job.setTitle(request.getTitle());
        job.setDescription(request.getDescription());
        job.setAddress(request.getAddress());
        job.setType(request.getType());
        job.setShiftType(request.getShiftType());
        job.setShiftLengthHours(request.getShiftLengthHours());
        
        // Save updated job
        jobRepository.save(job);
        
        // Handle keywords update if provided
        if (request.getKeywords() != null) {
            // Get existing keywords
            List<Keyword> existingKeywords = keywordRepository.findByKeywordOwner(job);
            
            // Remove existing keywords
            for (Keyword keyword : existingKeywords) {
                keywordRepository.delete(keyword);
            }
            
            // Add new keywords
            if (!request.getKeywords().isEmpty()) {
                addKeywordsToJob(request.getKeywords(), job);
            }
        }
        
        return job;
    }
}
