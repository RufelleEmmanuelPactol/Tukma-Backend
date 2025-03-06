package org.tukma.resume.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tukma.resume.models.Resume;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing Resume entities in the database.
 */
public interface ResumeRepository extends JpaRepository<Resume, Long> {
    
    /**
     * Find a resume by its hash value.
     *
     * @param resumeHash The hash identifying the resume
     * @return Optional containing the resume if found
     */
    Optional<Resume> findByResumeHash(String resumeHash);
    
    /**
     * Find all resumes associated with a specific job.
     *
     * @param jobId The job ID
     * @return List of resumes for the specified job
     */
    List<Resume> findByJob_Id(Long jobId);
    
    /**
     * Find all resumes uploaded by a specific user.
     *
     * @param ownerId The user ID
     * @return List of resumes uploaded by the specified user
     */
    List<Resume> findByOwner_Id(Long ownerId);
    
    /**
     * Find resumes by both job and owner.
     *
     * @param jobId The job ID
     * @param ownerId The user ID
     * @return List of resumes matching both criteria
     */
    List<Resume> findByJob_IdAndOwner_Id(Long jobId, Long ownerId);
    
    /**
     * Check if a resume with a specific hash exists.
     *
     * @param resumeHash The hash to check
     * @return true if a resume with the given hash exists
     */
    boolean existsByResumeHash(String resumeHash);
}
