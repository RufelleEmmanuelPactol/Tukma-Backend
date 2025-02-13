package org.tukma.interviewer.dto;


import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;
@Getter
@Setter
public class InterviewState implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String company;
    private final String role;
    private final List<String> technicalQuestions;
    private final long creationTime;
    private final String conversationHistory;

    public InterviewState() {
        this(null, null, null, null);
    }

    public InterviewState(String company, String role, List<String> technicalQuestions, String conversationHistory) {
        this.company = company;
        this.role = role;
        this.technicalQuestions = technicalQuestions;
        this.conversationHistory = conversationHistory;
        this.creationTime = System.currentTimeMillis();
    }

    // Getters
    public String getCompany() {
        return company;
    }

    public String getRole() {
        return role;
    }

    public List<String> getTechnicalQuestions() {
        return technicalQuestions;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public String getConversationHistory() {
        return conversationHistory;
    }
}