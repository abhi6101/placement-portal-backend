package com.abhi.authProject.model;

import lombok.Data;
import java.util.List;

@Data
public class PaperUploadDto {
    private String subject;
    private Integer semester;
    private String branch;
    private String year;
    // Add additional fields if necessary
}
