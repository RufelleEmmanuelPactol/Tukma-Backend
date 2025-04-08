package org.tukma.survey.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for survey answer submissions
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SurveyAnswerDto {
    private Long questionId;
    private Integer score;
    // Optional comment field can be added if needed
    private String comment;
}
