package org.tukma.jobs.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tukma.jobs.models.Question;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    
    List<Question> findByJobId(Long jobId);
    
    void deleteByJobId(Long jobId);
    
    void deleteAllByJobId(Long jobId);
    
    long countByJobId(Long jobId);
}
