package org.tukma.interview.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO representing a single message structure from the Flask API response.
 */
public class FlaskMessage {

    @JsonProperty("role")
    private String role;

    @JsonProperty("content")
    private String content;

    // Getters and Setters
    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}