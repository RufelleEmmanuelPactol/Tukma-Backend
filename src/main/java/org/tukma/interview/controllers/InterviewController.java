package org.tukma.interview.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tukma.auth.models.UserEntity;
import org.tukma.interview.dtos.MessageRequest;
import org.tukma.interview.dtos.MessageResponse;
import org.tukma.interview.services.MessageProcessingService;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/v1/interview")
public class InterviewController {

    private static final Logger logger = Logger.getLogger(InterviewController.class.getName());
    
    private final MessageProcessingService messageProcessingService;
    
    @Autowired
    public InterviewController(MessageProcessingService messageProcessingService) {
        this.messageProcessingService = messageProcessingService;
    }

    @PostMapping("/messages")
    public ResponseEntity<MessageResponse> processMessages(@RequestBody MessageRequest messageRequest) {
        // Get current authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity currentUser = null;
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserEntity) {
            currentUser = (UserEntity) auth.getPrincipal();
        }
        
        // Log the received messages
        logger.info("Received " + messageRequest.getMessages().size() + " messages from " 
            + (currentUser != null ? currentUser.getUsername() : "unauthenticated user"));
        
        // Process the messages using the service
        var processedMessages = messageProcessingService.processMessages(messageRequest.getMessages());
        
        // Create a response with the processed messages
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("receivedCount", messageRequest.getMessages().size());
        responseData.put("messages", processedMessages);
        
        MessageResponse response = new MessageResponse(
            "success", 
            "Successfully processed messages", 
            responseData
        );
        
        return ResponseEntity.ok(response);
    }
}
