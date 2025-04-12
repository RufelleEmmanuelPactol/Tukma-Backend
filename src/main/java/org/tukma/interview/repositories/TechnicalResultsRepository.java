package org.tukma.interview.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.tukma.interview.models.TechnicalResults;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for TechnicalResults entity operations.
 * Provides methods for storing and retrieving technical evaluation results.
 */
@Repository
public interface TechnicalResultsRepository extends JpaRepository<TechnicalResults, Long> {
    
    /**
     * Find technical results for a specific user
     * @param userId The user's ID
     * @return List of technical results ordered by creation date (newest first)
     */
    List<TechnicalResults> findByUser_IdOrderByCreatedAtDesc(Long userId);
    
    /**
     * Find technical results with scores above a certain threshold
     * @param threshold The minimum score threshold
     * @return List of technical results with scores above or equal to the threshold
     */
    List<TechnicalResults> findByScoreGreaterThanEqual(Integer threshold);
    
    /**
     * Find technical results with scores below a certain threshold
     * @param threshold The maximum score threshold
     * @return List of technical results with scores below or equal to the threshold
     */
    List<TechnicalResults> findByScoreLessThanEqual(Integer threshold);
    
    /**
     * Find technical results for a specific job access key
     * @param accessKey The job's access key
     * @return List of technical results ordered by creation date (newest first)
     */
    List<TechnicalResults> findByAccessKeyOrderByCreatedAtDesc(String accessKey);
    
    /**
     * Find technical results for a specific job
     * @param jobId The job's ID
     * @return List of technical results ordered by creation date (newest first)
     */
    List<TechnicalResults> findByJob_IdOrderByCreatedAtDesc(Long jobId);
    
    /**
     * Find technical results for a specific user and job access key
     * @param userId The user's ID
     * @param accessKey The job's access key
     * @return List of technical results ordered by creation date (newest first)
     */
    List<TechnicalResults> findByUser_IdAndAccessKeyOrderByCreatedAtDesc(Long userId, String accessKey);
    
    /**
     * Find the average score for a specific user across all their technical results
     * @param userId The user's ID
     * @return The average score or null if no results exist
     */
    @Query("SELECT AVG(t.score) FROM TechnicalResults t WHERE t.user.id = :userId")
    Double findAverageScoreByUser_Id(@Param("userId") Long userId);
    
    /**
     * Find the average score for a specific job access key across all applicants
     * @param accessKey The job's access key
     * @return The average score or null if no results exist
     */
    @Query("SELECT AVG(t.score) FROM TechnicalResults t WHERE t.accessKey = :accessKey")
    Double findAverageScoreByAccessKey(@Param("accessKey") String accessKey);
    
    /**
     * Find the average score for a specific user and job access key
     * @param userId The user's ID
     * @param accessKey The job's access key
     * @return The average score or null if no results exist
     */
    @Query("SELECT AVG(t.score) FROM TechnicalResults t WHERE t.user.id = :userId AND t.accessKey = :accessKey")
    Double findAverageScoreByUser_IdAndAccessKey(@Param("userId") Long userId, @Param("accessKey") String accessKey);
}
