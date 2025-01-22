package org.tukma.jobs.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.tukma.auth.models.UserEntity;

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

    @Column(nullable = false, unique = true)
    private String accessKey;






}
