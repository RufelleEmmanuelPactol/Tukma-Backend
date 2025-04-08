package org.tukma.survey.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.tukma.survey.models.Questions;
import org.tukma.survey.services.QuestionsService;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/api/v1/survey/questions")
public class QuestionsController {

    private final QuestionsService questionsService;

    @Autowired
    public QuestionsController(QuestionsService questionsService) {
        this.questionsService = questionsService;
    }

    /**
     * Get all survey questions
     * @return List of questions
     */
    @GetMapping
    public ResponseEntity<List<Questions>> getAllQuestions() {
        return ResponseEntity.ok(questionsService.getAllQuestions());
    }

    /**
     * Get a specific question by ID
     * @param id Question ID
     * @return The question if found
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getQuestionById(@PathVariable Long id) {
        Optional<Questions> questionOpt = questionsService.getQuestionById(id);
        return questionOpt.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Create a new survey question
     * @param requestBody Map containing the question text
     * @return Created question
     */
    @PostMapping
    public ResponseEntity<?> createQuestion(@RequestBody Map<String, String> requestBody) {
        String questionText = requestBody.get("questionText");
        if (questionText == null || questionText.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Question text is required"));
        }

        Questions question = questionsService.createQuestion(questionText);
        return ResponseEntity.status(HttpStatus.CREATED).body(question);
    }

    /**
     * Update an existing question
     * @param id Question ID
     * @param requestBody Map containing the updated question text
     * @return Updated question
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateQuestion(
            @PathVariable Long id,
            @RequestBody Map<String, String> requestBody) {
        
        String questionText = requestBody.get("questionText");
        if (questionText == null || questionText.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Question text is required"));
        }

        Optional<Questions> updatedQuestionOpt = questionsService.updateQuestion(id, questionText);
        return updatedQuestionOpt.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Delete a question
     * @param id Question ID
     * @return No content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteQuestion(@PathVariable Long id) {
        // Check if question exists before deleting
        if (!questionsService.getQuestionById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }

        questionsService.deleteQuestion(id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Search for questions containing the search term
     * @param term Term to search for
     * @return List of matching questions
     */
    @GetMapping("/search")
    public ResponseEntity<List<Questions>> searchQuestions(@RequestParam String term) {
        return ResponseEntity.ok(questionsService.searchQuestions(term));
    }
}
