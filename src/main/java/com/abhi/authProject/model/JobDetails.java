package com.abhi.authProject.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "jobdetails")
public class JobDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @NotBlank(message = "Apply link is required")
    @Column(name = "apply_link", nullable = false, columnDefinition = "TEXT")
    private String apply_link;

    @NotBlank(message = "Title is required")
    @Column(name = "title", nullable = false)
    private String title;

    @NotNull(message = "Last date is required")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")

    @Temporal(TemporalType.DATE)
    @Column(name = "last_date", nullable = false)
    private Date last_date;

    @NotBlank(message = "Company name is required")
    @Column(name = "company_name", nullable = false)
    private String company_name;

    @NotBlank(message = "Description is required")
    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Min(value = 0, message = "Salary must be non-negative")
    @Column(name = "salary", nullable = false)
    private int salary;

    @Column(name = "interview_details", nullable = false, columnDefinition = "TEXT")
    private String interview_details;

    // Branch/Semester filtering fields
    @ElementCollection
    @CollectionTable(name = "job_eligible_branches", joinColumns = @JoinColumn(name = "job_id"))
    @Column(name = "branch")
    private java.util.List<String> eligibleBranches = new java.util.ArrayList<>(); // ["IMCA", "MCA", "BCA"]

    @ElementCollection
    @CollectionTable(name = "job_eligible_semesters", joinColumns = @JoinColumn(name = "job_id"))
    @Column(name = "semester")
    private java.util.List<Integer> eligibleSemesters = new java.util.ArrayList<>(); // [1, 2, 3, ...]
}
