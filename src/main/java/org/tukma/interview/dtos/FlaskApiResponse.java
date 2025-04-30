package org.tukma.interview.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * DTO representing the overall structure of the response from the Flask API's
 * /get_messages endpoint.
 */
public class FlaskApiResponse {

    @JsonProperty("messages")
    private List<FlaskMessage> messages;

    // Getters and Setters
    public List<FlaskMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<FlaskMessage> messages) {
        this.messages = messages;
    }
}