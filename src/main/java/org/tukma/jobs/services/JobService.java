package org.tukma.jobs.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tukma.auth.models.UserEntity;
import org.tukma.jobs.dtos.JobCreateRequest;
import org.tukma.jobs.dtos.JobEditRequest;
import org.tukma.jobs.dtos.PagedJobsResponse;
import org.tukma.jobs.models.Job;
import org.tukma.jobs.models.Keyword;
import org.tukma.jobs.repositories.JobRepository;
import org.tukma.jobs.repositories.KeywordRepository;

import java.util.AbstractMap.SimpleEntry;

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
        job.setLocationType(request.getLocationType());

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

    /**
     * Deletes a job and all its associated keywords
     * Uses @Transactional to ensure that all operations complete as a single unit
     *
     * @param job The job to delete
     */
    @Transactional
    public void deleteJob(Job job) {
        if (job == null) {
            return;
        }
        
        // First, delete all associated keywords
        List<Keyword> keywords = keywordRepository.findByKeywordOwner(job);
        if (!keywords.isEmpty()) {
            keywordRepository.deleteAll(keywords);
        }
        
        // Then delete the job
        jobRepository.deleteById(job.getId());
    }

    /**
     * Deletes a job by ID and all its associated keywords
     * Uses @Transactional to ensure that all operations complete as a single unit
     *
     * @param id The ID of the job to delete
     */
    @Transactional
    public void deleteJob(Long id) {
        // First, delete all associated keywords
        List<Keyword> keywords = keywordRepository.findByKeywordOwner_Id(id);
        if (!keywords.isEmpty()) {
            keywordRepository.deleteAll(keywords);
        }
        
        // Then delete the job
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
     * Get paginated jobs with their associated keywords for a specific user, sorted by updatedAt in descending order
     *
     * @param user The user entity whose jobs should be fetched
     * @param page The page number (0-based)
     * @param size The page size
     * @return PagedJobsResponse containing jobs with keywords and pagination metadata
     */
    public PagedJobsResponse getPagedJobsWithKeywords(UserEntity user, int page, int size) {
        // Create pageable with sorting by updatedAt in descending order
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        
        // Fetch page of jobs
        Page<Job> jobsPage = jobRepository.findByOwner_Id(user.getId(), pageable);
        
        // Convert jobs to job+keywords map
        List<Map<String, Object>> jobsWithKeywords = new ArrayList<>();
        for (Job job : jobsPage.getContent()) {
            jobsWithKeywords.add(getJobWithKeywords(job));
        }
        
        // Create pagination metadata
        boolean hasNextPage = jobsPage.getNumber() < jobsPage.getTotalPages() - 1;
        PagedJobsResponse.PaginationMetadata metadata = new PagedJobsResponse.PaginationMetadata(
                jobsPage.getNumber(),
                jobsPage.getSize(),
                jobsPage.getTotalElements(),
                jobsPage.getTotalPages(),
                hasNextPage
        );
        
        // Create and return response
        return new PagedJobsResponse(jobsWithKeywords, metadata);
    }
    
    /**
     * Get all jobs with pagination for applicants, sorted by updatedAt in descending order (most recent first)
     *
     * @param page The page number (0-based)
     * @param size The page size
     * @return PagedJobsResponse containing all jobs with keywords and pagination metadata
     */
    public PagedJobsResponse getPagedJobsForApplicants(int page, int size) {
        // Create pageable with sorting by updatedAt in descending order
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        
        // Fetch page of all jobs
        Page<Job> jobsPage = jobRepository.findAll(pageable);
        
        // Convert jobs to job+keywords map
        List<Map<String, Object>> jobsWithKeywords = new ArrayList<>();
        for (Job job : jobsPage.getContent()) {
            jobsWithKeywords.add(getJobWithKeywords(job));
        }
        
        // Create pagination metadata
        boolean hasNextPage = jobsPage.getNumber() < jobsPage.getTotalPages() - 1;
        PagedJobsResponse.PaginationMetadata metadata = new PagedJobsResponse.PaginationMetadata(
                jobsPage.getNumber(),
                jobsPage.getSize(),
                jobsPage.getTotalElements(),
                jobsPage.getTotalPages(),
                hasNextPage
        );
        
        // Create and return response
        return new PagedJobsResponse(jobsWithKeywords, metadata);
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
    @Transactional
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
        job.setLocationType(request.getLocationType());
        
        // Save updated job
        jobRepository.save(job);
        
        // Handle keywords update if provided
        if (request.getKeywords() != null) {
            // Get existing keywords
            List<Keyword> existingKeywords = keywordRepository.findByKeywordOwner(job);
            
            // Remove existing keywords
            if (!existingKeywords.isEmpty()) {
                keywordRepository.deleteAll(existingKeywords);
            }
            
            // Add new keywords
            if (!request.getKeywords().isEmpty()) {
                addKeywordsToJob(request.getKeywords(), job);
            }
        }
        
        return job;
    }
    
    /**
     * Search for jobs based on semantic similarity to the query
     *
     * @param query The search query
     * @param page The page number (0-based)
     * @param size The page size
     * @return PagedJobsResponse containing jobs that match the search criteria
     */
    public PagedJobsResponse searchJobs(String query, int page, int size) {
        // First, get all jobs matching the query using the repository method
        List<Job> matchingJobs = jobRepository.findBySearchQuery(query);
        
        // For each job, calculate a semantic similarity score
        List<Map.Entry<Job, Double>> scoredJobs = new ArrayList<>();
        for (Job job : matchingJobs) {
            double score = calculateSemanticSimilarity(job, query);
            scoredJobs.add(new SimpleEntry<>(job, score));
        }
        
        // Sort by score in descending order
        scoredJobs.sort(Map.Entry.<Job, Double>comparingByValue().reversed());
        
        // Paginate the results
        int start = page * size;
        int end = Math.min(start + size, scoredJobs.size());
        List<Map.Entry<Job, Double>> pagedScoredJobs = 
                (start < scoredJobs.size()) ? scoredJobs.subList(start, end) : new ArrayList<>();
        
        // Convert to the expected format
        List<Map<String, Object>> jobsWithKeywords = new ArrayList<>();
        for (Map.Entry<Job, Double> entry : pagedScoredJobs) {
            Map<String, Object> jobMap = getJobWithKeywords(entry.getKey());
            // Add the semantic similarity score to the response
            jobMap.put("relevanceScore", entry.getValue());
            jobsWithKeywords.add(jobMap);
        }
        
        // Create pagination metadata
        int totalPages = (int) Math.ceil((double) scoredJobs.size() / size);
        boolean hasNextPage = page < totalPages - 1;
        PagedJobsResponse.PaginationMetadata metadata = new PagedJobsResponse.PaginationMetadata(
                page,
                size,
                scoredJobs.size(),
                totalPages,
                hasNextPage
        );
        
        return new PagedJobsResponse(jobsWithKeywords, metadata);
    }

    /**
     * Calculate semantic similarity between a job and a query
     * This is a simple implementation that could be enhanced with NLP or ML techniques
     *
     * @param job The job to compare
     * @param query The search query
     * @return A similarity score between 0 and 1
     */
    private double calculateSemanticSimilarity(Job job, String query) {
        query = query.toLowerCase();
        String jobTitle = job.getTitle().toLowerCase();
        String jobDescription = job.getDescription().toLowerCase();
        
        // Get keywords for this job
        List<Keyword> keywords = keywordRepository.findByKeywordOwner(job);
        
        double score = 0.0;
        
        // Title match (weighted higher)
        if (jobTitle.contains(query)) {
            score += 0.5;
        }
        
        // Description match
        if (jobDescription.contains(query)) {
            score += 0.3;
        }
        
        // Keyword matches
        for (Keyword keyword : keywords) {
            if (keyword.getKeywordName().toLowerCase().contains(query) ||
                query.contains(keyword.getKeywordName().toLowerCase())) {
                score += 0.2;
                break;  // Count only once for keywords
            }
        }
        
        // Advanced scoring: partial word matches for title and keywords
        String[] queryWords = query.split("\\s+");
        for (String word : queryWords) {
            if (word.length() > 3) {  // Only consider meaningful words
                if (jobTitle.contains(word)) {
                    score += 0.1;
                }
                
                for (Keyword keyword : keywords) {
                    if (keyword.getKeywordName().toLowerCase().contains(word)) {
                        score += 0.1;
                        break;  // Count only once per word for keywords
                    }
                }
            }
        }
        
        // Cap the score at 1.0
        return Math.min(score, 1.0);
    }
}
