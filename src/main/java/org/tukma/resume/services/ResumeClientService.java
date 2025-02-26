package org.tukma.resume.services;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.tukma.resume.dtos.ProcessingStatusResponse;
import org.tukma.resume.dtos.ResumeUploadResponse;
import org.tukma.resume.dtos.SimilarityScoreResponse;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class ResumeClientService {
    private final WebClient webClient;
    private static final String BASE_URL = "https://ai.tukma.work";

    public ResumeClientService() {
        this.webClient = WebClient.builder()
                .baseUrl(BASE_URL)
                .build();
    }

    public Mono<ResumeUploadResponse> uploadResume(byte[] resumeBytes, List<String> keywords) {
        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("resume", new ByteArrayResource(resumeBytes))
                .filename("resume.pdf")
                .contentType(MediaType.APPLICATION_PDF);

        keywords.forEach(keyword -> bodyBuilder.part("keyword", keyword));

        return webClient.post()
                .uri("/api/v1/resume-service")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                .retrieve()
                .bodyToMono(ResumeUploadResponse.class)
                .onErrorResume(throwable ->
                        Mono.error(new RuntimeException("Failed to upload resume: " + throwable.getMessage())));
    }

    public Mono<SimilarityScoreResponse> getSimilarityScore(String hash) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/similarity-score")
                        .queryParam("applicant-hash", hash)
                        .build())
                .retrieve()
                .bodyToMono(SimilarityScoreResponse.class)
                .onErrorResume(throwable ->
                        Mono.error(new RuntimeException("Failed to get similarity score: " + throwable.getMessage())));
    }

    public Mono<ProcessingStatusResponse> checkProcessingStatus(String hash) {
        System.out.println("Checking status for hash: " + hash);
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/check-status")
                        .queryParam("applicant-hash", hash)
                        .build())
                .retrieve()
                .bodyToMono(ProcessingStatusResponse.class)
                .doOnError(e -> System.err.println("Error checking status: " + e.getMessage()))
                .onErrorResume(throwable ->
                        Mono.error(new RuntimeException("Failed to check status: " + throwable.getMessage())));
    }
}
