package org.tukma.resume.controllers;

/**
 * Controller handling resume upload and processing operations.
 * Provides endpoints for uploading resumes, checking processing status,
 * and retrieving similarity scores.
 */

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.tukma.resume.dtos.ProcessingStatusResponse;
import org.tukma.resume.dtos.ResumeUploadRequest;
import org.tukma.resume.dtos.ResumeUploadResponse;
import org.tukma.resume.dtos.SimilarityScoreResponse;
import org.tukma.resume.services.ResumeClientService;

import java.io.IOException;

/**
 * REST controller for resume-related operations.
 * Base path: /api/v1/resume
 */
@Controller
@Validated
@RequestMapping("/api/v1/resume")
public class ResumeController {

    private final ResumeClientService resumeClientService;

    @Autowired
    public ResumeController(ResumeClientService resumeClientService) {
        this.resumeClientService = resumeClientService;
    }

    /**
 * Uploads a resume file with associated keywords for processing.
 *
 * @param request Contains the resume file (PDF) and list of keywords for analysis
 * @return ResponseEntity containing a hash identifier for tracking the upload
 * @throws IOException if there are issues reading the resume file
 */
@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResumeUploadResponse> uploadResume(@Valid @ModelAttribute ResumeUploadRequest request) 
            throws IOException {
        return ResponseEntity.ok(
            resumeClientService.uploadResume(request.getResume().getBytes(), request.getKeywords())
                .block()
        );
    }

    /**
 * Retrieves the similarity score for a previously uploaded resume.
 *
 * @param hash The unique identifier returned from the upload endpoint
 * @return ResponseEntity containing the similarity analysis results
 */
@GetMapping("/score/{hash}")
    public ResponseEntity<SimilarityScoreResponse> getSimilarityScore(@PathVariable String hash) {
        return ResponseEntity.ok(
            resumeClientService.getSimilarityScore(hash)
                .block()
        );
    }

    /**
 * Checks the processing status of a resume upload.
 *
 * @param hash The unique identifier returned from the upload endpoint
 * @return ResponseEntity containing the current processing status
 */
@GetMapping("/status/{hash}")
    public ResponseEntity<ProcessingStatusResponse> checkStatus(@PathVariable String hash) {
        return ResponseEntity.ok(
            resumeClientService.checkProcessingStatus(hash)
                .block()
        );
    }
}
