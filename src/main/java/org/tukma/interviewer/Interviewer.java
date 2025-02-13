package org.tukma.interviewer;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.core.env.Environment;
import okhttp3.*;
import java.io.IOException;
import java.util.*;

public class Interviewer {

    private static final String LLM_ENDPOINT = "https://api.openai.com/v1/chat/completions";
    private final Environment environment;
    private String company;
    private String role;
    private List<String> technicalQuestions;
    private boolean hasStartedInterview;
    private String systemPrompt;
    private final List<HashMap<String, String>> conversationHistory; // Uses HashMap

    private final OkHttpClient client;
    private final Gson gson;

    public Interviewer(Environment environment) {
        this.environment = environment;
        this.client = new OkHttpClient();
        this.gson = new Gson();
        this.conversationHistory = new ArrayList<>();
        this.hasStartedInterview = false;
    }

    public String startInterview(String company, String role, List<String> technicalQuestions) throws IOException {
        if (hasStartedInterview) {
            throw new IllegalStateException("Interview has already started.");
        }

        this.company = company;
        this.role = role;
        this.technicalQuestions = technicalQuestions;
        this.systemPrompt = StaticPrompts.generateSystemPrompt(technicalQuestions, company, role);
        hasStartedInterview = true;

        // Initialize conversation history with system message using HashMap
        HashMap<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);
        conversationHistory.clear();
        conversationHistory.add(systemMessage);
        return askQuestion("");
    }

    public String askQuestion(String question) throws IOException {
        if (!hasStartedInterview) {
            throw new IllegalStateException("Interview has not started yet.");
        }

        HashMap<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", question);
        conversationHistory.add(userMessage);

        String response = sendRequestToLLM();

        HashMap<String, String> assistantMessage = new HashMap<>();
        assistantMessage.put("role", "assistant");
        assistantMessage.put("content", response);
        conversationHistory.add(assistantMessage);
        System.out.println("FINISH API QUERY");
        return response;
    }

    private String sendRequestToLLM() throws IOException {
        // Create request payload using HashMap
        HashMap<String, Object> requestPayload = new HashMap<>();
        System.out.println("CONVERSATION SIZE: " + conversationHistory.size());
        if (conversationHistory.size() % 15 == 0 && !conversationHistory.isEmpty()) {
            conversationHistory.add((HashMap<String, String>) Map.of("role", "system", "content", "Maybe consider moving to technical after this? If you have finished technical, consider ending the interview sooner or later."));
        }
        requestPayload.put("model", "gpt-4o");
        requestPayload.put("messages", conversationHistory);
        requestPayload.put("temperature", 0.7);
        requestPayload.put("max_tokens", 2048);

        RequestBody body = RequestBody.create(
                gson.toJson(requestPayload),
                MediaType.get("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(LLM_ENDPOINT)
                .addHeader("Authorization", "Bearer " + getAPIKey())
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                System.err.println(response);
                throw new IOException("Unexpected response: " + response);
            }

            // Parse response using Gson
            Map<String, Object> jsonResponse = gson.fromJson(response.body().string(), Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) jsonResponse.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                return message.get("content").toString();
            }
            return "No response from AI.";
        }
    }

    public String getAPIKey() {
        return environment.getProperty("openai.key");
    }

    public void endInterview() {
        hasStartedInterview = false;

    }
}