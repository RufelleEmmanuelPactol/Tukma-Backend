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
}
