package com.abhi.authProject.model;

import lombok.Data;

@Data
public class VisionAIResponseDto {
    private boolean isQualityGood;
    private String subject;
    private Integer semester;
    private String branch;
    private String year;
    private String message;
}
