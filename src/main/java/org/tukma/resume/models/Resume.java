package org.tukma.resume.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.tukma.auth.models.UserEntity;
import org.tukma.jobs.models.Job;

@Entity
@Getter
@Setter
@Table(name = "resumes")
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String resumeHash;

    @Column(columnDefinition = "TEXT")
    private String results;

    @ManyToOne
    @JoinColumn(name = "job_id", referencedColumnName = "id", nullable = false)
    private Job job;

    @ManyToOne
    @JoinColumn(name = "owner_id", referencedColumnName = "id", nullable = false)
    private UserEntity owner;
}
