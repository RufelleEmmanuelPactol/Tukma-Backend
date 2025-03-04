package org.tukma.jobs.dtos;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.tukma.jobs.models.Job.JobType;
import org.tukma.jobs.models.Job.ShiftType;

@Getter
@Setter
public class JobCreateRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String description;
    
    @NotBlank(message = "Job address is required")
    private String address;
    
    @NotNull(message = "Job type is required")
    private JobType type = JobType.FULL_TIME;
    
    private ShiftType shiftType;
    
    @Positive(message = "If provided, shift length must be positive")
    private Integer shiftLengthHours;
}
