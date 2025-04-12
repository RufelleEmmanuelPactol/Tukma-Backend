package org.tukma.survey.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.tukma.auth.models.UserEntity;
import org.tukma.survey.dtos.SurveyStatisticsDto;
import org.tukma.survey.models.Answer;
import org.tukma.survey.models.Questions;
import org.tukma.survey.services.AnswerService;
import org.tukma.survey.services.QuestionsService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/api/v1/survey/answers")
public class AnswerController {

    private final AnswerService answerService;
    private final QuestionsService questionsService;

    @Autowired
    public AnswerController(AnswerService answerService, QuestionsService questionsService) {
        this.answerService = answerService;
        this.questionsService = questionsService;
    }

    /**
     * Get all answers (admin only)
     * @return List of all answers
     */
    @GetMapping
    public ResponseEntity<?> getAllAnswers() {
        // In a production environment, this should be restricted to admins
        return ResponseEntity.ok(answerService.getAllAnswers());
    }

    /**
     * Get answers for the currently authenticated user
     * @return List of user's answers
     */
    @GetMapping("/me")
    public ResponseEntity<?> getMyAnswers() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "You must be logged in to view your answers"));
        }
        
        UserEntity currentUser = (UserEntity) auth.getPrincipal();
        return ResponseEntity.ok(answerService.getAnswersByUserId(currentUser.getId()));
    }

    /**
     * Get answers for a specific question
     * @param questionId Question ID
     * @return List of answers or statistics
     */
    @GetMapping("/question/{questionId}")
    public ResponseEntity<?> getAnswersByQuestion(@PathVariable Long questionId) {
        // In a production environment, this should be restricted to admins
        List<Answer> answers = answerService.getAnswersByQuestionId(questionId);
        
        // Calculate statistics
        Double averageScore = answerService.getAverageScoreForQuestion(questionId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("answers", answers);
        response.put("count", answers.size());
        response.put("averageScore", averageScore);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Submit a single answer
     * @param requestBody Map containing questionId and score
     * @return Created answer
     */
    @PostMapping
    public ResponseEntity<?> submitAnswer(@RequestBody Map<String, Object> requestBody) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "You must be logged in to submit answers"));
        }
        
        UserEntity currentUser = (UserEntity) auth.getPrincipal();
        
        try {
            Long questionId = ((Number) requestBody.get("questionId")).longValue();
            Integer score = ((Number) requestBody.get("score")).intValue();
            
            if (score < 1 || score > 5) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Score must be between 1 and 5"));
            }
            
            Answer answer = answerService.createAnswer(questionId, currentUser, score);
            
            // Determine if this was a creation or update based on the timestamp
            HttpStatus status = answer.getCreatedAt().equals(answer.getUpdatedAt()) 
                ? HttpStatus.CREATED : HttpStatus.OK;
                
            return ResponseEntity.status(status).body(answer);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An error occurred: " + e.getMessage()));
        }
    }

    /**
     * Submit multiple answers (complete survey)
     * @param requestBody Map containing a list of answers (each with questionId and score)
     * @return List of created answers
     */
    @PostMapping("/submit-survey")
    public ResponseEntity<?> submitSurvey(@RequestBody Map<String, Object> requestBody) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "You must be logged in to submit a survey"));
        }
        
        UserEntity currentUser = (UserEntity) auth.getPrincipal();
        
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> answers = (List<Map<String, Object>>) requestBody.get("answers");
            
            if (answers == null || answers.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "No answers provided"));
            }
            
            // Validate all answers
            for (Map<String, Object> answer : answers) {
                if (!answer.containsKey("questionId") || !answer.containsKey("score")) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "Each answer must have questionId and score"));
                }
                
                Integer score = ((Number) answer.get("score")).intValue();
                if (score < 1 || score > 5) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "Scores must be between 1 and 5"));
                }
            }
            
            List<Answer> savedAnswers = answerService.submitSurvey(answers, currentUser);
            return ResponseEntity.status(HttpStatus.OK).body(Map.of(
                "message", "Survey submitted successfully",
                "answers", savedAnswers
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An error occurred: " + e.getMessage()));
        }
    }

    /**
     * Update an existing answer
     * @param id Answer ID
     * @param requestBody Map containing new score
     * @return Updated answer
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateAnswer(
            @PathVariable Long id,
            @RequestBody Map<String, Object> requestBody) {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "You must be logged in to update answers"));
        }
        
        UserEntity currentUser = (UserEntity) auth.getPrincipal();
        
        try {
            // Verify ownership
            Optional<Answer> answerOpt = answerService.getAnswerById(id);
            if (answerOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Answer not found"));
            }
            
            Answer answer = answerOpt.get();
            if (!answer.getUser().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You can only update your own answers"));
            }
            
            // Update score
            Integer score = ((Number) requestBody.get("score")).intValue();
            Answer updatedAnswer = answerService.updateAnswer(id, score);
            return ResponseEntity.ok(updatedAnswer);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An error occurred: " + e.getMessage()));
        }
    }

    /**
     * Delete an answer
     * @param id Answer ID
     * @return No content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAnswer(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "You must be logged in to delete answers"));
        }
        
        UserEntity currentUser = (UserEntity) auth.getPrincipal();
        
        try {
            // Verify ownership
            Optional<Answer> answerOpt = answerService.getAnswerById(id);
            if (answerOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Answer not found"));
            }
            
            Answer answer = answerOpt.get();
            if (!answer.getUser().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You can only delete your own answers"));
            }
            
            // Delete answer
            answerService.deleteAnswer(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An error occurred: " + e.getMessage()));
        }
    }

    /**
     * Get statistics for all questions
     * @return Map of question IDs to statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<?> getStatistics() {
        // In a production environment, this might be restricted to admins
        
        try {
            // Get all questions using the QuestionsService
            List<Questions> questions = questionsService.getAllQuestions();
            List<SurveyStatisticsDto> statisticsList = new ArrayList<>();
            
            for (Questions question : questions) {
                Long questionId = question.getId();
                List<Answer> answers = answerService.getAnswersByQuestionId(questionId);
                Double averageScore = answerService.getAverageScoreForQuestion(questionId);
                
                // Calculate score distribution
                int score1 = 0, score2 = 0, score3 = 0, score4 = 0, score5 = 0;
                for (Answer answer : answers) {
                    switch (answer.getScore()) {
                        case 1: score1++; break;
                        case 2: score2++; break;
                        case 3: score3++; break;
                        case 4: score4++; break;
                        case 5: score5++; break;
                    }
                }
                
                SurveyStatisticsDto stats = new SurveyStatisticsDto(
                    question,
                    averageScore,
                    answers.size(),
                    score1,
                    score2,
                    score3,
                    score4,
                    score5
                );
                
                statisticsList.add(stats);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("statistics", statisticsList);
            response.put("totalQuestions", questions.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An error occurred: " + e.getMessage()));
        }
    }
    
    /**
     * Calculate the System Usability Scale (SUS) score for a user
     * SUS is a standard measure of perceived usability
     * 
     * @return SUS score (0-100) and interpretation
     */
    @GetMapping("/sus-score")
    public ResponseEntity<?> getSusScore() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "You must be logged in to calculate SUS score"));
        }
        
        UserEntity currentUser = (UserEntity) auth.getPrincipal();
        
        try {
            Map<String, Object> susScore = answerService.calculateSusScore(currentUser.getId());
            return ResponseEntity.ok(susScore);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An error occurred: " + e.getMessage()));
        }
    }
    
    /**
     * Get overall SUS statistics across all users (admin only)
     * 
     * @return SUS statistics
     */
    @GetMapping("/overall-sus-statistics")
    public ResponseEntity<?> getOverallSusStatistics() {
        // In a production environment, this should be restricted to admins
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "You must be logged in to view overall statistics"));
        }
        
        try {
            Map<String, Object> statistics = answerService.calculateOverallSusStatistics();
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An error occurred: " + e.getMessage()));
        }
    }
    
    /**
     * Check if the current user has completed the SUS survey (answered at least 10 unique questions)
     * 
     * @return Survey completion status
     */
    @GetMapping("/check-survey-completion")
    public ResponseEntity<?> checkSurveyCompletion() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "You must be logged in to check survey completion"));
        }
        
        UserEntity currentUser = (UserEntity) auth.getPrincipal();
        
        try {
            // Get the SUS score which includes the number of answered questions
            Map<String, Object> susData = answerService.calculateSusScore(currentUser.getId());
            Integer answeredQuestions = (Integer) susData.get("answeredQuestions");
            
            // A standard SUS survey consists of 10 questions
            boolean isComplete = answeredQuestions >= 10;
            
            Map<String, Object> response = new HashMap<>();
            response.put("isComplete", isComplete);
            response.put("answeredQuestions", answeredQuestions);
            response.put("requiredQuestions", 10);
            response.put("remainingQuestions", Math.max(0, 10 - answeredQuestions));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An error occurred: " + e.getMessage()));
        }
    }
}
