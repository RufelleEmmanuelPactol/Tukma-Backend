package org.tukma.jobs.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.tukma.jobs.models.Job;

import java.util.List;
import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, Long> {

    public List<Job> findByOwner_Id(Long id);
    
    public Page<Job> findByOwner_Id(Long id, Pageable pageable);

    public Optional<Job> findByAccessKey(String accessKey);

    public boolean existsByAccessKey(String accessKey);
    
    /**
     * Find jobs where title or description contains the search query (case insensitive)
     */
    Page<Job> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String title, String description, Pageable pageable);
    
    /**
     * Advanced search query that searches across multiple fields with relevance scoring
     */
    @Query(value = "SELECT j.* FROM jobs j LEFT JOIN keyword k ON j.id = k.keyword_owner_id " +
            "WHERE LOWER(j.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(j.description) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(k.keyword_name) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "GROUP BY j.id", nativeQuery = true)
    List<Job> findBySearchQuery(@Param("query") String query);
}
