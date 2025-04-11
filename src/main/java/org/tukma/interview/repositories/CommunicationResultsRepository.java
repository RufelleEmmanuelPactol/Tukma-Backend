package org.tukma.interview.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.tukma.interview.models.CommunicationResults;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for CommunicationResults entity operations.
 * Provides methods for storing and retrieving communication evaluation results.
 */
@Repository
public interface CommunicationResultsRepository extends JpaRepository<CommunicationResults, Long> {
    
    /**
     * Find communication results for a specific user
     * @param userId The user's ID
     * @return List of communication results ordered by creation date (newest first)
     */
    List<CommunicationResults> findByUser_IdOrderByCreatedAtDesc(Long userId);
    
    /**
     * Find the most recent communication result for a user
     * @param userId The user's ID
     * @return Optional containing the most recent result if available
     */
    Optional<CommunicationResults> findFirstByUser_IdOrderByCreatedAtDesc(Long userId);
    
    /**
     * Find communication results with scores above a certain threshold
     * @param threshold The minimum score threshold
     * @return List of communication results with scores above the threshold
     */
    List<CommunicationResults> findByOverallScoreGreaterThanEqual(Double threshold);
    
    /**
     * Find communication results with scores below a certain threshold
     * @param threshold The maximum score threshold
     * @return List of communication results with scores below the threshold
     */
    List<CommunicationResults> findByOverallScoreLessThanEqual(Double threshold);
}