package com.abhi.authProject.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DepartmentWithBranchesDto {
    private Long id;
    private String name;
    private String code;
    private String category;
    private String hodName;
    private String contactEmail;
    private String description;
    private List<BranchDto> branches;
}
