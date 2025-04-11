package org.tukma.interview.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.tukma.auth.models.UserEntity;

import java.time.LocalDateTime;

/**
 * Entity class for storing communication evaluation results from interviews.
 * This captures metrics related to a user's communication skills assessment.
 */
@Entity
@Getter
@Setter
@Table(name = "communication_results")
public class CommunicationResults {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private UserEntity user;

    @Column(nullable = false)
    private Double overallScore;

    @Column(columnDefinition = "TEXT")
    private String strengths;

    @Column(columnDefinition = "TEXT")
    private String areasForImprovement;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}