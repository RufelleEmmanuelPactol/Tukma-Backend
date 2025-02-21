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

    @NotEmpty(message = "At least one keyword is required")
    private List<String> keywords;
}
