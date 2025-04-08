package org.tukma.survey.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tukma.survey.models.Questions;

import java.util.List;

public interface QuestionsRepository extends JpaRepository<Questions, Long> {
    
    // Find questions ordered by creation date (newest first)
    List<Questions> findAllByOrderByCreatedAtDesc();
    
    // Find questions containing specific text (for searching)
    List<Questions> findByQuestionTextContainingIgnoreCase(String searchTerm);
}
