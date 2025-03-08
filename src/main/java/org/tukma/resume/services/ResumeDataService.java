package org.tukma.resume.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tukma.auth.models.UserEntity;
import org.tukma.auth.repositories.UserRepository;
import org.tukma.jobs.models.Job;
import org.tukma.jobs.repositories.JobRepository;
import org.tukma.resume.models.Resume;
import org.tukma.resume.repositories.ResumeRepository;
import org.tukma.resume.utils.ResumeResultParser;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for managing resume data in the local database
 */
@Service
public class ResumeDataService {

    private final ResumeRepository resumeRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;

    @Autowired
    public ResumeDataService(ResumeRepository resumeRepository, JobRepository jobRepository, UserRepository userRepository) {
        this.resumeRepository = resumeRepository;
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
    }

    /**
     * Save or update a resume entity with the given hash and results
     * 
     * @param resumeHash The unique hash identifying the resume
     * @param rawResults The raw results string from the microservice (can be null)
     * @param jobId The ID of the job the resume is being applied to
     * @param userId The ID of the user who owns the resume
     * @return The saved Resume entity
     */
    public Resume saveResumeData(String resumeHash, String rawResults, Long jobId, Long userId) {
        // Parse the Python-formatted results to JSON if results are provided
        String jsonResults = null;
        if (rawResults != null && !rawResults.isEmpty()) {
            jsonResults = ResumeResultParser.pythonToJson(rawResults);
        }
        
        // Check if a resume with this hash already exists
        Optional<Resume> existingResume = resumeRepository.findByResumeHash(resumeHash);
        
        if (existingResume.isPresent()) {
            // Update existing resume if results are provided
            if (jsonResults != null) {
                Resume resume = existingResume.get();
                resume.setResults(jsonResults);
                return resumeRepository.save(resume);
            }
            return existingResume.get();
        } else {
            // Create new resume
            Resume resume = new Resume();
            resume.setResumeHash(resumeHash);
            resume.setResults(jsonResults);
            
            // Get job and owner entities
            Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found with ID: " + jobId));
            
            UserEntity owner = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
            
            resume.setJob(job);
            resume.setOwner(owner);
            
            return resumeRepository.save(resume);
        }
    }

    /**
     * Get resume data by hash
     * 
     * @param resumeHash The unique hash identifying the resume
     * @return Optional containing the Resume entity if found
     */
    public Optional<Resume> getResumeByHash(String resumeHash) {
        return resumeRepository.findByResumeHash(resumeHash);
    }
    
    /**
     * Get all resumes for a specific job
     * 
     * @param jobId The ID of the job
     * @return List of resumes
     */
    public List<Resume> getResumesByJob(Long jobId) {
        return resumeRepository.findByJob_Id(jobId);
    }
    
    /**
     * Get all resumes for a specific user
     * 
     * @param userId The ID of the user
     * @return List of resumes
     */
    public List<Resume> getResumesByUser(Long userId) {
        return resumeRepository.findByOwner_Id(userId);
    }
    
    /**
     * Parse stored resume results into a Map
     * 
     * @param resume The resume entity containing JSON results
     * @return Map of parsed results or empty map if no results
     */
    public Map<String, Map<String, Object>> parseResumeResults(Resume resume) {
        if (resume.getResults() == null || resume.getResults().isEmpty()) {
            return Map.of();
        }
        
        try {
            // We'll use Gson to parse the stored JSON back to a Map
            return new com.google.gson.Gson().fromJson(
                resume.getResults(), 
                new com.google.gson.reflect.TypeToken<Map<String, Map<String, Object>>>(){}.getType()
            );
        } catch (Exception e) {
            return Map.of();
        }
    }
    
    /**
     * Get resumes submitted by a specific user for a specific job
     * 
     * @param jobId The ID of the job
     * @param userId The ID of the user
     * @return List of resumes
     */
    public List<Resume> getResumesByJobAndUser(Long jobId, Long userId) {
        return resumeRepository.findAllByJob_IdAndOwner_Id(jobId, userId);
    }
    
    /**
     * Get a single resume submitted by a specific user for a specific job
     * 
     * @param jobId The ID of the job
     * @param userId The ID of the user
     * @return Optional containing the Resume entity if found
     */
    public Optional<Resume> getResumeByJobAndUser(Long jobId, Long userId) {
        return resumeRepository.findByJob_IdAndOwner_Id(jobId, userId);
    }
}
