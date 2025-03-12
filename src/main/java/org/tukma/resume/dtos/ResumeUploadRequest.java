package org.tukma.resume.dtos;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class ResumeUploadRequest {
    @NotNull(message = "Resume file is required")
    private MultipartFile resume;

    /**
     * Keywords are optional for job-specific uploads where keywords are extracted from the job.
     * They are required for general uploads without a job reference.
     */
    private List<String> keywords;
}
