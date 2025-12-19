package com.abhi.authProject.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private String name; // e.g., "Master of Computer Applications"

    @Column(nullable = false, unique = true)
    private String code; // e.g., "MCA"

    @Column(name = "hod_name")
    private String hodName;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "max_semesters")
    private int maxSemesters = 8;
}
