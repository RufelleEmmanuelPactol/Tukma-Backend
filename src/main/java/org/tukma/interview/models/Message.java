package org.tukma.interview.models;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    private Long id;
    private String content;
    private String timestamp; // ISO-8601 formatted timestamp
    private String role; // e.g., "user", "system"
}
