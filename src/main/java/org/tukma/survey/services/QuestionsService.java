package org.tukma.survey.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tukma.survey.models.Questions;
import org.tukma.survey.repositories.QuestionsRepository;

import java.util.List;
import java.util.Optional;

@Service
public class QuestionsService {

    private final QuestionsRepository questionsRepository;

    @Autowired
    public QuestionsService(QuestionsRepository questionsRepository) {
        this.questionsRepository = questionsRepository;
    }

    /**
     * Get all survey questions ordered by creation date (newest first)
     * @return List of questions
     */
    public List<Questions> getAllQuestions() {
        return questionsRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * Get a specific question by ID
     * @param id Question ID
     * @return Optional containing the question if found
     */
    public Optional<Questions> getQuestionById(Long id) {
        return questionsRepository.findById(id);
    }

    /**
     * Create a new survey question
     * @param questionText The text of the question
     * @return Created question
     */
    @Transactional
    public Questions createQuestion(String questionText) {
        Questions question = new Questions();
        question.setQuestionText(questionText.trim());
        return questionsRepository.save(question);
    }

    /**
     * Update an existing question
     * @param id Question ID
     * @param questionText New question text
     * @return Updated question
     */
    @Transactional
    public Optional<Questions> updateQuestion(Long id, String questionText) {
        Optional<Questions> questionOpt = questionsRepository.findById(id);
        if (questionOpt.isPresent()) {
            Questions question = questionOpt.get();
            question.setQuestionText(questionText.trim());
            return Optional.of(questionsRepository.save(question));
        }
        return Optional.empty();
    }

    /**
     * Delete a question
     * @param id Question ID
     */
    @Transactional
    public void deleteQuestion(Long id) {
        questionsRepository.deleteById(id);
    }
    
    /**
     * Search for questions containing the search term
     * @param searchTerm Term to search for
     * @return List of matching questions
     */
    public List<Questions> searchQuestions(String searchTerm) {
        return questionsRepository.findByQuestionTextContainingIgnoreCase(searchTerm);
    }
}
