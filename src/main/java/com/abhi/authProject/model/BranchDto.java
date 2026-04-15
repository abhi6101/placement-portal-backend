package com.abhi.authProject.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BranchDto {
    private Long id;
    private String branchName;
    private String branchCode;
    private String degree;
    private Integer maxSemesters;
    private String hodName;
    private String contactEmail;
    private String description;
}
