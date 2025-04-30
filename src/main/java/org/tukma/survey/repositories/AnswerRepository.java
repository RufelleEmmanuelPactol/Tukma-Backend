package org.tukma.survey.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.tukma.survey.models.Answer;

import java.util.List;

public interface AnswerRepository extends JpaRepository<Answer, Long> {

    // Find answers by user ID
    List<Answer> findByUser_Id(Long userId);

    // Find answers by question ID
    List<Answer> findByQuestion_Id(Long questionId);

    // Find answers by user ID and question ID
    List<Answer> findByUser_IdAndQuestion_Id(Long userId, Long questionId);

    // Find all answers ordered by creation date (newest first)
    List<Answer> findAllByOrderByCreatedAtDesc();

    @Query("SELECT DISTINCT a.user.id FROM Answer a")
    List<Long> findDistinctUserIds();
}
