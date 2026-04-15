package com.abhi.authProject.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Entity
@Table(name = "departments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name; // e.g., "School of Computer", "B.Tech Engineering"

    @Column(nullable = false, unique = true)
    private String code; // e.g., "SOC", "BTECH"

    @Column(name = "category")
    private String category; // e.g., "Computer Science", "Engineering"

    @Column(name = "hod_name")
    private String hodName; // Department-level HOD (for SOC, one HOD for all branches)

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "description", length = 500)
    private String description;

    // One-to-many relationship with branches
    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL, orphanRemoval = true)
    @lombok.ToString.Exclude
    private List<DepartmentBranch> branches;
}
