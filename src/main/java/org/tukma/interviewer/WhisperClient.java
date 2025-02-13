package org.tukma.interviewer;


import com.nimbusds.jose.shaded.gson.Gson;
import okhttp3.*;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
public class WhisperClient {

    Environment environment;
    private static final String API_URL = "https://api.openai.com/v1/audio/speech";

    public String getAPIKey() {
        return environment.getProperty("openai.key");
    }


    public WhisperClient(Environment environment) {
        this.environment = environment;
    }


    public CompletableFuture<byte[]> generateSpeech(String text) {
        return CompletableFuture.supplyAsync(() -> {
            OkHttpClient client = new OkHttpClient();
            Map<String, Object> params = new HashMap<>();
            params.put("model", "tts-1");
            params.put("input", text);
            params.put("voice", "sage");

            Gson gson = new Gson();
            String json = gson.toJson(params);

            RequestBody body = RequestBody.create(
                    json, MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(API_URL)
                    .addHeader("Authorization", "Bearer " + getAPIKey())
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                }

                // Read the audio file as a byte array
                try (InputStream inputStream = response.body().byteStream();
                     ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    return outputStream.toByteArray();
                }
            } catch (IOException e) {
                throw new RuntimeException("Error generating speech", e);
            }
        });
    }




}
