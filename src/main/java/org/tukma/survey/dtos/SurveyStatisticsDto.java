package org.tukma.survey.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.tukma.survey.models.Questions;

/**
 * DTO for survey question statistics
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SurveyStatisticsDto {
    private Questions question;
    private Double averageScore;
    private Integer responseCount;
    private Integer scoreDistribution1;
    private Integer scoreDistribution2;
    private Integer scoreDistribution3;
    private Integer scoreDistribution4;
    private Integer scoreDistribution5;
}
