package com.abhi.authProject.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "interview_drives")
public class InterviewDrive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String company;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private String time;

    @Column(nullable = false)
    private String venue;

    private String eligibility;

    // Stored as comma-separated string for simplicity
    private String positions; 

    private int totalSlots;
    private int bookedSlots;

    public InterviewDrive() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }
    public String getEligibility() { return eligibility; }
    public void setEligibility(String eligibility) { this.eligibility = eligibility; }
    public String getPositions() { return positions; }
    public void setPositions(String positions) { this.positions = positions; }
    public int getTotalSlots() { return totalSlots; }
    public void setTotalSlots(int totalSlots) { this.totalSlots = totalSlots; }
    public int getBookedSlots() { return bookedSlots; }
    public void setBookedSlots(int bookedSlots) { this.bookedSlots = bookedSlots; }
}
