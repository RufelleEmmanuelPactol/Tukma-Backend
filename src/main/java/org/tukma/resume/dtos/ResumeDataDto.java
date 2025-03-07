package org.tukma.resume.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.tukma.auth.models.UserEntity;
import org.tukma.jobs.models.Job;
import org.tukma.resume.models.Resume;

import java.util.Map;

/**
 * DTO for returning resume data with parsed results
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResumeDataDto {
    private Long id;
    private String resumeHash;
    private Job job;
    private UserEntity owner;
    private Map<String, Map<String, Object>> parsedResults;
    
    /**
     * Create DTO from Resume entity and parsed results
     */
    public static ResumeDataDto fromResume(Resume resume, Map<String, Map<String, Object>> parsedResults) {
        ResumeDataDto dto = new ResumeDataDto();
        dto.setId(resume.getId());
        dto.setResumeHash(resume.getResumeHash());
        dto.setJob(resume.getJob());
        dto.setOwner(resume.getOwner());
        dto.setParsedResults(parsedResults);
        return dto;
    }
}
