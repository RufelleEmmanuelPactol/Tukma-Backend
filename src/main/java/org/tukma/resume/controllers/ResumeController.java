package org.tukma.resume.controllers;

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

@Controller
@Validated
@RequestMapping("/api/v1/resume")
public class ResumeController {

    private final ResumeClientService resumeClientService;

    @Autowired
    public ResumeController(ResumeClientService resumeClientService) {
        this.resumeClientService = resumeClientService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResumeUploadResponse> uploadResume(@Valid @ModelAttribute ResumeUploadRequest request) 
            throws IOException {
        return ResponseEntity.ok(
            resumeClientService.uploadResume(request.getResume().getBytes(), request.getKeywords())
                .block()
        );
    }

    @GetMapping("/score/{hash}")
    public ResponseEntity<SimilarityScoreResponse> getSimilarityScore(@PathVariable String hash) {
        return ResponseEntity.ok(
            resumeClientService.getSimilarityScore(hash)
                .block()
        );
    }

    @GetMapping("/status/{hash}")
    public ResponseEntity<ProcessingStatusResponse> checkStatus(@PathVariable String hash) {
        return ResponseEntity.ok(
            resumeClientService.checkProcessingStatus(hash)
                .block()
        );
    }
}
