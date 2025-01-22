package org.tukma.jobs.dtos;


import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JobCreateRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String description;

}
