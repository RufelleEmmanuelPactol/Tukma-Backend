package org.tukma.jobs.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Keyword {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private String keywordName;

    @ManyToOne
    @JoinColumn(name = "keyword_owner_id",referencedColumnName = "id")
    private Job keywordOwner;
}
