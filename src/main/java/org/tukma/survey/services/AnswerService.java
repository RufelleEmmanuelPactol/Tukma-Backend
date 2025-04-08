package org.tukma.survey.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tukma.auth.models.UserEntity;
import org.tukma.auth.repositories.UserRepository;
import org.tukma.survey.models.Answer;
import org.tukma.survey.models.Questions;
import org.tukma.survey.repositories.AnswerRepository;
import org.tukma.survey.repositories.QuestionsRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AnswerService {

    private final AnswerRepository answerRepository;
    private final QuestionsRepository questionsRepository;
    private final UserRepository userRepository;

    @Autowired
    public AnswerService(
            AnswerRepository answerRepository, 
            QuestionsRepository questionsRepository,
            UserRepository userRepository) {
        this.answerRepository = answerRepository;
        this.questionsRepository = questionsRepository;
        this.userRepository = userRepository;
    }

    /**
     * Get all answers
     * @return List of answers
     */
    public List<Answer> getAllAnswers() {
        return answerRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * Get a specific answer by ID
     * @param id Answer ID
     * @return Optional containing the answer if found
     */
    public Optional<Answer> getAnswerById(Long id) {
        return answerRepository.findById(id);
    }

    /**
     * Get answers by user ID
     * @param userId User ID
     * @return List of answers
     */
    public List<Answer> getAnswersByUserId(Long userId) {
        return answerRepository.findByUser_Id(userId);
    }

    /**
     * Get answers by question ID
     * @param questionId Question ID
     * @return List of answers
     */
    public List<Answer> getAnswersByQuestionId(Long questionId) {
        return answerRepository.findByQuestion_Id(questionId);
    }

    /**
     * Create a new answer or update an existing one
     * @param questionId Question ID
     * @param user User entity
     * @param score Score (typically 1-5 for SUS)
     * @return Created or updated answer
     * @throws IllegalArgumentException if question not found or score out of range
     */
    @Transactional
    public Answer createAnswer(Long questionId, UserEntity user, Integer score) {
        if (score < 1 || score > 5) {
            throw new IllegalArgumentException("Score must be between 1 and 5");
        }

        Questions question = questionsRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found with ID: " + questionId));

        // Check if user already has an answer for this question
        List<Answer> existingAnswers = answerRepository.findByUser_IdAndQuestion_Id(user.getId(), questionId);
        Answer answer;
        
        if (!existingAnswers.isEmpty()) {
            // Update existing answer
            answer = existingAnswers.get(0);
            answer.setScore(score);
        } else {
            // Create new answer
            answer = new Answer();
            answer.setQuestion(question);
            answer.setUser(user);
            answer.setScore(score);
        }
        
        return answerRepository.save(answer);
    }

    /**
     * Update an existing answer
     * @param id Answer ID
     * @param score New score
     * @return Updated answer
     * @throws IllegalArgumentException if answer not found or score out of range
     */
    @Transactional
    public Answer updateAnswer(Long id, Integer score) {
        if (score < 1 || score > 5) {
            throw new IllegalArgumentException("Score must be between 1 and 5");
        }

        Answer answer = answerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Answer not found with ID: " + id));
        
        answer.setScore(score);
        return answerRepository.save(answer);
    }

    /**
     * Delete an answer
     * @param id Answer ID
     */
    @Transactional
    public void deleteAnswer(Long id) {
        answerRepository.deleteById(id);
    }

    /**
     * Calculate average score for a question
     * @param questionId Question ID
     * @return Average score or null if no answers
     */
    public Double getAverageScoreForQuestion(Long questionId) {
        List<Answer> answers = answerRepository.findByQuestion_Id(questionId);
        
        if (answers.isEmpty()) {
            return null;
        }
        
        double sum = answers.stream()
                .mapToInt(Answer::getScore)
                .sum();
                
        return sum / answers.size();
    }

    /**
     * Submit multiple answers at once (for a complete survey)
     * Existing answers will be updated, new ones will be created
     * 
     * @param answersData List of maps containing questionId and score
     * @param user User entity
     * @return List of created or updated answers
     */
    @Transactional
    public List<Answer> submitSurvey(List<java.util.Map<String, Object>> answersData, UserEntity user) {
        List<Answer> savedAnswers = new java.util.ArrayList<>();
        
        for (java.util.Map<String, Object> answerData : answersData) {
            Long questionId = ((Number) answerData.get("questionId")).longValue();
            Integer score = ((Number) answerData.get("score")).intValue();
            
            // createAnswer method now handles both creation and update
            Answer answer = createAnswer(questionId, user, score);
            savedAnswers.add(answer);
        }
        
        return savedAnswers;
    }
    
    /**
     * Calculate SUS score for a specific user
     * SUS is calculated using a standard formula:
     * - For odd-numbered questions: (score - 1)
     * - For even-numbered questions: (5 - score)
     * - Sum all values and multiply by 2.5 to get a score out of 100
     * 
     * @param userId User ID
     * @return Map containing SUS score and additional information
     */
    public Map<String, Object> calculateSusScore(Long userId) {
        List<Answer> userAnswers = answerRepository.findByUser_Id(userId);
        
        if (userAnswers.isEmpty()) {
            return Map.of(
                "susScore", 0.0,
                "interpretation", "No data",
                "answeredQuestions", 0,
                "possibleMax", 100.0
            );
        }
        
        // Group answers by question ID to avoid duplicates
        Map<Long, Answer> questionAnswers = userAnswers.stream()
            .collect(Collectors.toMap(
                answer -> answer.getQuestion().getId(),
                answer -> answer,
                // If duplicate answers for the same question, keep the most recent one
                (a1, a2) -> a1.getCreatedAt().isAfter(a2.getCreatedAt()) ? a1 : a2
            ));
        
        double totalScore = 0;
        
        for (Map.Entry<Long, Answer> entry : questionAnswers.entrySet()) {
            Long questionId = entry.getKey();
            Answer answer = entry.getValue();
            int score = answer.getScore();
            
            // SUS formula: For odd-numbered questions, subtract 1 from the user response
            // For even-numbered questions, subtract the user response from 5
            boolean isOddQuestion = questionId % 2 == 1;
            
            if (isOddQuestion) {
                totalScore += (score - 1);
            } else {
                totalScore += (5 - score);
            }
        }
        
        // SUS formula: Multiply the sum of the scores by 2.5 to get a score out of 100
        double susScore = totalScore * 2.5;
        
        // Adjust the score based on the number of questions (standard SUS has 10 questions)
        int questionCount = questionAnswers.size();
        if (questionCount != 10) {
            susScore = (susScore / questionCount) * 10;
        }
        
        // Interpret the score
        String interpretation;
        if (susScore >= 85) {
            interpretation = "Excellent";
        } else if (susScore >= 72) {
            interpretation = "Good";
        } else if (susScore >= 52) {
            interpretation = "OK";
        } else if (susScore >= 38) {
            interpretation = "Poor";
        } else {
            interpretation = "Awful";
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("susScore", Math.round(susScore * 10.0) / 10.0); // Round to 1 decimal place
        result.put("interpretation", interpretation);
        result.put("answeredQuestions", questionCount);
        result.put("possibleMax", 100.0);
        
        return result;
    }
    
    /**
     * Calculate overall SUS statistics for the application
     * 
     * @return Map containing overall statistics
     */
    public Map<String, Object> calculateOverallSusStatistics() {
        List<UserEntity> users = userRepository.findAll();
        List<Map<String, Object>> userScores = new java.util.ArrayList<>();
        double totalSusScore = 0;
        int userCount = 0;
        
        // Score distribution
        int excellent = 0, good = 0, ok = 0, poor = 0, awful = 0;
        
        for (UserEntity user : users) {
            Map<String, Object> userScore = calculateSusScore(user.getId());
            Double susScore = (Double) userScore.get("susScore");
            
            // Only count users who have answered at least one question
            if ((Integer) userScore.get("answeredQuestions") > 0) {
                userCount++;
                totalSusScore += susScore;
                
                // Track score distribution
                if (susScore >= 85) excellent++;
                else if (susScore >= 72) good++;
                else if (susScore >= 52) ok++;
                else if (susScore >= 38) poor++;
                else awful++;
                
                // Add user details if needed (may want to exclude for privacy)
                Map<String, Object> userScoreData = new HashMap<>();
                userScoreData.put("userId", user.getId());
                userScoreData.put("username", user.getUsername());
                userScoreData.put("susScore", susScore);
                userScoreData.put("interpretation", userScore.get("interpretation"));
                
                userScores.add(userScoreData);
            }
        }
        
        // Calculate average SUS score
        double averageSusScore = (userCount > 0) ? totalSusScore / userCount : 0;
        
        // Overall interpretation
        String overallInterpretation;
        if (averageSusScore >= 85) {
            overallInterpretation = "Excellent";
        } else if (averageSusScore >= 72) {
            overallInterpretation = "Good";
        } else if (averageSusScore >= 52) {
            overallInterpretation = "OK";
        } else if (averageSusScore >= 38) {
            overallInterpretation = "Poor";
        } else {
            overallInterpretation = "Awful";
        }
        
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("averageSusScore", Math.round(averageSusScore * 10.0) / 10.0);
        statistics.put("totalRespondents", userCount);
        statistics.put("overallInterpretation", overallInterpretation);
        
        // Score distribution
        Map<String, Object> distribution = new HashMap<>();
        distribution.put("excellent", excellent);
        distribution.put("good", good);
        distribution.put("ok", ok);
        distribution.put("poor", poor);
        distribution.put("awful", awful);
        
        statistics.put("scoreDistribution", distribution);
        statistics.put("userScores", userScores);
        
        return statistics;
    }
}
