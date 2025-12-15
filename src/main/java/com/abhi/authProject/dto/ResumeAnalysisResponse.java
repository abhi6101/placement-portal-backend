package com.abhi.authProject.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResumeAnalysisResponse {
    private int score;
    private String summary;
    private List<String> strengths;
    private List<String> weaknesses;
    private List<String> missingKeywords;
    private String recommendedRole;
}
