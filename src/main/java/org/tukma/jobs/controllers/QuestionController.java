package org.tukma.jobs.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.tukma.auth.models.UserEntity;
import org.tukma.jobs.models.Job;
import org.tukma.jobs.models.Question;
import org.tukma.jobs.services.JobService;
import org.tukma.jobs.services.QuestionService;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/api/v1/jobs/questions")
public class QuestionController {

    private final JobService jobService;
    private final QuestionService questionService;

    @Autowired
    public QuestionController(JobService jobService, QuestionService questionService) {
        this.jobService = jobService;
        this.questionService = questionService;
    }

    /**
     * Get all questions for a job
     * @param accessKey Job access key
     * @return List of questions
     */
    @GetMapping("/{accessKey}")
    public ResponseEntity<?> getQuestions(@PathVariable String accessKey) {
        Job job = jobService.getByAccessKey(accessKey);
        if (job == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Job not found with access key: " + accessKey));
        }

        List<Question> questions = questionService.getQuestionsByJobId(job.getId());
        return ResponseEntity.ok(questions);
    }
    
    /**
     * Get the count of questions for a job
     * @param accessKey Job access key
     * @return Question count
     */
    @GetMapping("/{accessKey}/count")
    public ResponseEntity<?> getQuestionCount(@PathVariable String accessKey) {
        Job job = jobService.getByAccessKey(accessKey);
        if (job == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Job not found with access key: " + accessKey));
        }

        long count = questionService.getQuestionCountByJobId(job.getId());
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Add a new question to a job
     * @param accessKey Job access key
     * @param questionText Question text in the request body
     * @return Created question
     */
    @PostMapping("/{accessKey}")
    public ResponseEntity<?> addQuestion(@PathVariable String accessKey, @RequestBody Map<String, String> request) {
        Job job = jobService.getByAccessKey(accessKey);
        if (job == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Job not found with access key: " + accessKey));
        }

        // Check if user is authorized to modify this job
        UserEntity currentUser = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!job.getOwner().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "You are not authorized to modify this job"));
        }

        String questionText = request.get("questionText");
        if (questionText == null || questionText.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Question text is required"));
        }

        Question question = questionService.addQuestion(job, questionText);
        return ResponseEntity.status(HttpStatus.CREATED).body(question);
    }
    
    /**
     * Add multiple questions to a job
     * @param accessKey Job access key
     * @param request List of questions in the request body
     * @return Created questions
     */
    @PostMapping("/{accessKey}/batch")
    public ResponseEntity<?> addQuestions(
            @PathVariable String accessKey,
            @RequestBody Map<String, List<String>> request) {
        
        Job job = jobService.getByAccessKey(accessKey);
        if (job == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Job not found with access key: " + accessKey));
        }

        // Check if user is authorized to modify this job
        UserEntity currentUser = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!job.getOwner().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "You are not authorized to modify this job"));
        }

        List<String> questionTexts = request.get("questions");
        if (questionTexts == null || questionTexts.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "At least one question is required"));
        }

        List<Question> questions = questionService.addQuestions(job, questionTexts);
        return ResponseEntity.status(HttpStatus.CREATED).body(questions);
    }

    /**
     * Update an existing question
     * @param questionId Question ID
     * @param request Request body with updated question text
     * @return Updated question
     */
    @PutMapping("/{accessKey}/{questionId}")
    public ResponseEntity<?> updateQuestion(
            @PathVariable String accessKey,
            @PathVariable Long questionId,
            @RequestBody Map<String, String> request) {
        
        Job job = jobService.getByAccessKey(accessKey);
        if (job == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Job not found with access key: " + accessKey));
        }

        // Check if user is authorized to modify this job
        UserEntity currentUser = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!job.getOwner().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "You are not authorized to modify this job"));
        }

        String questionText = request.get("questionText");
        if (questionText == null || questionText.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Question text is required"));
        }

        Question question = questionService.getQuestionById(questionId);
        if (question == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Question not found with ID: " + questionId));
        }

        // Ensure question belongs to specified job
        if (!question.getJob().getId().equals(job.getId())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Question does not belong to this job"));
        }

        question = questionService.updateQuestion(questionId, questionText);
        return ResponseEntity.ok(question);
    }

    /**
     * Delete a question
     * @param accessKey Job access key
     * @param questionId Question ID
     * @return No content
     */
    @DeleteMapping("/{accessKey}/{questionId}")
    public ResponseEntity<?> deleteQuestion(
            @PathVariable String accessKey,
            @PathVariable Long questionId) {
        
        Job job = jobService.getByAccessKey(accessKey);
        if (job == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Job not found with access key: " + accessKey));
        }

        // Check if user is authorized to modify this job
        UserEntity currentUser = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!job.getOwner().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "You are not authorized to modify this job"));
        }

        Question question = questionService.getQuestionById(questionId);
        if (question == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Question not found with ID: " + questionId));
        }

        // Ensure question belongs to specified job
        if (!question.getJob().getId().equals(job.getId())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Question does not belong to this job"));
        }

        questionService.deleteQuestion(questionId);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Delete all questions for a job
     * @param accessKey Job access key
     * @return No content
     */
    @DeleteMapping("/{accessKey}/all")
    public ResponseEntity<?> deleteAllQuestions(@PathVariable String accessKey) {
        Job job = jobService.getByAccessKey(accessKey);
        if (job == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Job not found with access key: " + accessKey));
        }

        // Check if user is authorized to modify this job
        UserEntity currentUser = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!job.getOwner().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "You are not authorized to modify this job"));
        }

        questionService.deleteQuestionsByJobId(job.getId());
        return ResponseEntity.noContent().build();
    }
}
