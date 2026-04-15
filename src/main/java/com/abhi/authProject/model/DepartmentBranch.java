package com.abhi.authProject.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "department_branches")
public class DepartmentBranch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(name = "branch_name", nullable = false)
    private String branchName; // "Master of Computer Applications"

    @Column(name = "branch_code", nullable = false, unique = true)
    private String branchCode; // "MCA"

    @Column(name = "degree")
    private String degree; // "MCA", "BCA", "B.Tech"

    @Column(name = "max_semesters", nullable = false)
    private Integer maxSemesters; // 4, 6, 8, 10

    @Column(name = "hod_name")
    private String hodName; // Branch-specific HOD (optional, for B.Tech branches)

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "description", length = 500)
    private String description;
}
