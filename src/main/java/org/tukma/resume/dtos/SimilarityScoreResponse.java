package org.tukma.resume.dtos;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SimilarityScoreResponse {
    private String hash;
    private Object result;  // Using Object since the result structure wasn't specified in the API doc
}
