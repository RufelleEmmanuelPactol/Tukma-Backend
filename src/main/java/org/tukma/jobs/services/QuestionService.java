package org.tukma.jobs.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tukma.jobs.models.Job;
import org.tukma.jobs.models.Question;
import org.tukma.jobs.models.Question.QuestionType;
import org.tukma.jobs.repositories.QuestionRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class QuestionService {

    private final QuestionRepository questionRepository;

    @Autowired
    public QuestionService(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    /**
     * Get all questions for a job
     * @param jobId Job ID
     * @return List of questions
     */
    public List<Question> getQuestionsByJobId(Long jobId) {
        return questionRepository.findByJobId(jobId);
    }

    /**
     * Get a question by ID
     * @param questionId Question ID
     * @return Question or null if not found
     */
    public Question getQuestionById(Long questionId) {
        Optional<Question> questionOpt = questionRepository.findById(questionId);
        return questionOpt.orElse(null);
    }

    /**
     * Add a new question to a job
     * @param job Job entity
     * @param questionText Question text
     * @param type Question type (TECHNICAL or BEHAVIORAL)
     * @return Created question
     */
    @Transactional
    public Question addQuestion(Job job, String questionText, QuestionType type) {
        Question question = new Question();
        question.setJob(job);
        question.setQuestionText(questionText.trim());
        question.setType(type);
        return questionRepository.save(question);
    }
    
    /**
     * Add multiple questions to a job
     * @param job Job entity
     * @param questionTexts List of question texts
     * @param type Question type (TECHNICAL or BEHAVIORAL)
     * @return List of created questions
     */
    @Transactional
    public List<Question> addQuestions(Job job, List<String> questionTexts, QuestionType type) {
        List<Question> questions = new ArrayList<>();
        
        for (String questionText : questionTexts) {
            if (questionText != null && !questionText.trim().isEmpty()) {
                Question question = new Question();
                question.setJob(job);
                question.setQuestionText(questionText.trim());
                question.setType(type);
                questions.add(question);
            }
        }
        
        if (!questions.isEmpty()) {
            return questionRepository.saveAll(questions);
        }
        return questions;
    }

    /**
     * Update an existing question
     * @param questionId Question ID
     * @param questionText New question text
     * @param type Question type (TECHNICAL or BEHAVIORAL)
     * @return Updated question
     */
    @Transactional
    public Question updateQuestion(Long questionId, String questionText, QuestionType type) {
        Optional<Question> questionOpt = questionRepository.findById(questionId);
        if (questionOpt.isPresent()) {
            Question question = questionOpt.get();
            question.setQuestionText(questionText.trim());
            question.setType(type);
            return questionRepository.save(question);
        }
        return null;
    }

    /**
     * Delete a question
     * @param questionId Question ID
     */
    @Transactional
    public void deleteQuestion(Long questionId) {
        questionRepository.deleteById(questionId);
    }
    
    /**
     * Delete all questions for a job
     * @param jobId Job ID
     */
    @Transactional
    public void deleteQuestionsByJobId(Long jobId) {
        questionRepository.deleteAllByJobId(jobId);
    }
    
    /**
     * Get the count of questions for a job
     * @param jobId Job ID
     * @return Count of questions
     */
    public long getQuestionCountByJobId(Long jobId) {
        return questionRepository.countByJobId(jobId);
    }
}
