package org.tukma.jobs.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.tukma.auth.models.UserEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.util.List;

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

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false, unique = true)
    private String accessKey;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobType type = JobType.FULL_TIME;
    
    @Enumerated(EnumType.STRING)
    @Column()
    private ShiftType shiftType;
    
    @Column()
    private Integer shiftLengthHours;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LocationType locationType = LocationType.ON_SITE;
    
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
    
    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Question> questions;
    
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
    
    public enum LocationType {
        REMOTE,
        HYBRID,
        ON_SITE
    }
}
