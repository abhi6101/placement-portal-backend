package com.abhi.authProject.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StudentGroupedDto {
    private String groupBy; // "branch", "semester", "batch"
    private Map<String, List<UserDto>> groups; // e.g., {"MCA": [students], "BCA": [students]}
    private int totalStudents;
}
