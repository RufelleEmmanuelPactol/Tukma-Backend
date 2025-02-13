package org.tukma.utils;
import org.tukma.interviewer.dto.InterviewState;
import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.tukma.interviewer.Interviewer;
import org.tukma.interviewer.WhisperClient;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/debug")
public class TestAndDebugging {

    private final Environment environment;
    private final ResourceLoader resourceLoader;
    private final WhisperClient whisperClient;
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String REDIS_KEY_PREFIX = "interview:";
    private static final long INTERVIEW_TIMEOUT = 3600; // 1 hour in seconds

    public TestAndDebugging(Environment environment, ResourceLoader resourceLoader,
                            WhisperClient whisperClient, RedisTemplate<String, Object> redisTemplate) {
        this.environment = environment;
        this.resourceLoader = resourceLoader;
        this.whisperClient = whisperClient;
        this.redisTemplate = redisTemplate;
    }

    private String getUserKey(Authentication auth) {
        return REDIS_KEY_PREFIX + auth.getName();
    }

    @PostMapping(value = "/interview-start", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter startInterview(@RequestBody Map<String, Object> payload) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            SseEmitter emitter = new SseEmitter();
            sendError(emitter, "User not authenticated");
            emitter.complete();
            return emitter;
        }

        SseEmitter emitter = new SseEmitter();

        try {
            // Extract interview details
            String company = (String) payload.get("company");
            String role = (String) payload.get("role");
            List<String> technicalQuestions = (List<String>) payload.get("technicalQuestions");

            if (company == null || role == null || technicalQuestions == null || technicalQuestions.isEmpty()) {
                emitter.send(SseEmitter.event()
                        .data(Map.of("error", "Invalid request: Provide company, role, and technicalQuestions."))
                        .build());
                emitter.complete();
                return emitter;
            }

            // Create a new interviewer
            Interviewer interviewer = new Interviewer(environment);

            CompletableFuture.supplyAsync(() -> {
                        try {
                            String response = interviewer.startInterview(company, role, technicalQuestions);

                            // Store interview state in Redis
                            InterviewState state = new InterviewState(
                                    company,
                                    role,
                                    technicalQuestions,
                                    response // This is our conversation history
                            );
                            String userKey = getUserKey(auth);
                            redisTemplate.opsForValue().set(userKey, state, INTERVIEW_TIMEOUT, TimeUnit.SECONDS);

                            return response;
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .thenApply(this::extractMessages)
                    .thenAccept(messages -> {
                        try {
                            processAndStreamResponses(messages, emitter);
                        } catch (IOException e) {
                            emitter.completeWithError(e);
                        }
                    })
                    .exceptionally(e -> {
                        sendError(emitter, e.getMessage());
                        emitter.complete();
                        return null;
                    });

        } catch (Exception e) {
            sendError(emitter, e.getMessage());
            emitter.complete();
        }

        return emitter;
    }

    @PostMapping(value = "/interview-ask", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter askInterviewQuestion(@RequestBody Map<String, String> payload) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        SseEmitter emitter = new SseEmitter();

        if (auth == null || !auth.isAuthenticated()) {
            sendError(emitter, "User not authenticated");
            emitter.complete();
            return emitter;
        }

        String userKey = getUserKey(auth);
        Object stateObj = redisTemplate.opsForValue().get(userKey);

        if (stateObj == null) {
            sendError(emitter, "No active interview found. Please start a new interview.");
            emitter.complete();
            return emitter;
        }

        try {
            String question = payload.get("response");
            if (question == null || question.isBlank()) {
                sendError(emitter, "Invalid request: Provide a question.");
                emitter.complete();
                return emitter;
            }

            InterviewState state = (InterviewState) stateObj;

            // Recreate interviewer with saved state
            Interviewer interviewer = new Interviewer(environment);
            interviewer.startInterview(
                    state.getCompany(),
                    state.getRole(),
                    state.getTechnicalQuestions()
            );

            CompletableFuture.supplyAsync(() -> {
                        try {
                            // Get the response
                            String response = interviewer.askQuestion(question);

                            // Update state in Redis
                            InterviewState newState = new InterviewState(
                                    state.getCompany(),
                                    state.getRole(),
                                    state.getTechnicalQuestions(),
                                    response
                            );
                            redisTemplate.opsForValue().set(userKey, newState, INTERVIEW_TIMEOUT, TimeUnit.SECONDS);

                            return response;
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .thenApply(this::extractMessages)
                    .thenAccept(messages -> {
                        try {
                            processAndStreamResponses(messages, emitter);
                        } catch (IOException e) {
                            emitter.completeWithError(e);
                        }
                    })
                    .exceptionally(e -> {
                        sendError(emitter, e.getMessage());
                        emitter.complete();
                        return null;
                    });

        } catch (Exception e) {
            sendError(emitter, e.getMessage());
            emitter.complete();
        }

        return emitter;
    }

    // ... rest of the methods remain the same ...

    private void processAndStreamResponses(List<String> messages, SseEmitter emitter) throws IOException {
        List<CompletableFuture<Void>> tasks = new ArrayList<>();

        for (int i = 0; i < messages.size(); i++) {
            final int order = i;
            String message = messages.get(i);

            CompletableFuture<Void> task = whisperClient.generateSpeech(message)
                    .thenAccept(audioData -> {
                        Map<String, Object> response = Map.of(
                                "order", order,
                                "message", message,
                                "audioBase64", encodeToBase64(audioData)
                        );
                        try {
                            emitter.send(SseEmitter.event()
                                    .data(response)
                                    .build());
                        } catch (IOException e) {
                            emitter.completeWithError(e);
                        }
                    });

            tasks.add(task);
        }

        CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0]))
                .thenRun(emitter::complete)
                .exceptionally(e -> {
                    emitter.completeWithError(e);
                    return null;
                });
    }

    private List<String> extractMessages(String jsonResponse) {
        try {
            Gson gson = new Gson();
            Map<String, Object> parsedResponse = gson.fromJson(jsonResponse, Map.class);
            return (List<String>) parsedResponse.get("messages");
        } catch (Exception e) {
            return List.of("Error parsing response: " + e.getMessage());
        }
    }

    private void sendError(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event()
                    .data(Map.of("error", message))
                    .build());
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }

    private static String encodeToBase64(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }
}