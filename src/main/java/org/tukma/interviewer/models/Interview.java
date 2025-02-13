package org.tukma.interviewer.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.tukma.auth.models.UserEntity;
import org.tukma.jobs.models.Job;

@Entity
@Getter
@Setter
public class Interview {

    @Id
    @GeneratedValue
    Long id;

    String key;

    @ManyToOne
    @JoinColumn(name="user_id", referencedColumnName = "id", nullable = false)
    UserEntity user;

    @ManyToOne
    @JoinColumn(name="job_id", referencedColumnName = "id", nullable = true)
    Job job;

    String analysisResults; // from ai.tukma.work/resume-service
    String resumeText; // the actual raw text from the resume

    String ticket;
}
