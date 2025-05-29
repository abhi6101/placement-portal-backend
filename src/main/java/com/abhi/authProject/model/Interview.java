package com.abhi.authProject.model;


import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "interviews")
public class Interview {
    

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne // One JobApplication can have one Interview
    @JoinColumn(name = "job_application_id", referencedColumnName = "id", nullable = false)
    private JobApplication jobApplication;

    @Column(nullable = false)
    private String hrName; // Name of the HR scheduling
    @Column(nullable = false)
    private String hrEmail; // Email of the HR

    @Column(nullable = false)
    private LocalDateTime scheduledDateTime; // Proposed by HR

    private String interviewLink; // For online interviews
    private String interviewLocation; // For in-person interviews
    private String notes; // HR notes for the interview

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterviewStatus status; // SCHEDULED, BOOKED, COMPLETED, CANCELLED

    private LocalDateTime studentBookedDateTime; // Actual date/time student confirms

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Constructor, Getters, Setters

    public Interview() {
        this.createdAt = LocalDateTime.now();
        this.status = InterviewStatus.SCHEDULED; // Default status
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public JobApplication getJobApplication() { return jobApplication; }
    public void setJobApplication(JobApplication jobApplication) { this.jobApplication = jobApplication; }
    public String getHrName() { return hrName; }
    public void setHrName(String hrName) { this.hrName = hrName; }
    public String getHrEmail() { return hrEmail; }
    public void setHrEmail(String hrEmail) { this.hrEmail = hrEmail; }
    public LocalDateTime getScheduledDateTime() { return scheduledDateTime; }
    public void setScheduledDateTime(LocalDateTime scheduledDateTime) { this.scheduledDateTime = scheduledDateTime; }
    public String getInterviewLink() { return interviewLink; }
    public void setInterviewLink(String interviewLink) { this.interviewLink = interviewLink; }
    public String getInterviewLocation() { return interviewLocation; }
    public void setInterviewLocation(String interviewLocation) { this.interviewLocation = interviewLocation; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public InterviewStatus getStatus() { return status; }
    public void setStatus(InterviewStatus status) { this.status = status; }
    public LocalDateTime getStudentBookedDateTime() { return studentBookedDateTime; }
    public void setStudentBookedDateTime(LocalDateTime studentBookedDateTime) { this.studentBookedDateTime = studentBookedDateTime; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Interview{" +
               "id=" + id +
               ", jobApplication=" + (jobApplication != null ? jobApplication.getId() : "null") +
               ", hrName='" + hrName + '\'' +
               ", scheduledDateTime=" + scheduledDateTime +
               ", status=" + status +
               '}';
    }
}