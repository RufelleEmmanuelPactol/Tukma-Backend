package org.tukma.jobs.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.tukma.auth.models.UserEntity;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name="jobs")
@ToString
public class Job {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    @JoinColumn(name="owner_id", referencedColumnName = "id")
    private UserEntity owner;

    @Column(name = "description", nullable = false)
    private String description; // Maps to 'description' column, not 'job_description'

    @Column(name = "title", nullable = false)
    private String title; // Maps to 'title' column, not 'job_title'

    @Column(name = "access_key", nullable = false, unique = true)
    private String accessKey;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false)
    private JobType type = JobType.FULL_TIME;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "shift_type")
    private ShiftType shiftType;
    
    @Column(name = "shift_length_hours")
    private Integer shiftLengthHours;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
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
    
    public enum JobType {
        FULL_TIME,
        PART_TIME,
        INTERNSHIP,
        CONTRACT
    }
    
    public enum ShiftType {
        DAY_SHIFT,
        NIGHT_SHIFT,
        ROTATING_SHIFT,
        FLEXIBLE_SHIFT
    }
}
