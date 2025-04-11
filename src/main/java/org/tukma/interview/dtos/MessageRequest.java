package org.tukma.interview.dtos;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.tukma.interview.models.Message;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageRequest {
    private List<Message> messages;
    
    /**
     * Optional job access key for linking results to a specific job application
     */
    private String accessKey;
    
    /**
     * Constructor with just messages
     * @param messages The list of messages
     */
    public MessageRequest(List<Message> messages) {
        this.messages = messages;
        this.accessKey = null;
    }
}
