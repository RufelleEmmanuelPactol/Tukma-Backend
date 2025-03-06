package org.tukma.resume.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.tukma.auth.models.UserEntity;
import org.tukma.jobs.models.Job;
import org.tukma.resume.converters.ResumeResultConverter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Entity to store resume analysis results.
 */
@Entity
@Table(name = "resumes")
@Getter
@Setter
public class Resume {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String resumeHash;
    
    @Column(columnDefinition = "TEXT")
    @Convert(converter = ResumeResultConverter.class)
    private Map<String, Object> result;
    
    @ManyToOne
    @JoinColumn(name = "job_id", referencedColumnName = "id", nullable = false)
    private Job job;
    
    @ManyToOne
    @JoinColumn(name = "owner_id", referencedColumnName = "id", nullable = false)
    private UserEntity owner;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
