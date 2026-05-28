package com.abhi.authProject.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Date;

@Entity
@Table(name = "paper_view_logs")
public class PaperViewLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String studentName;
    private String computerCode;
    
    private Long paperId;
    private String paperTitle;
    private String subject;
    private String branch;
    private int semester;
    private int year;
    
    private Date viewedAt;
    
    @Column(nullable = false, length = 20)
    private String action = "VIEW"; // Defaults to VIEW for backward compatibility

    public PaperViewLog() {
        this.viewedAt = new Date();
    }

    public PaperViewLog(String username, String studentName, String computerCode, Long paperId, 
                        String paperTitle, String subject, String branch, int semester, int year,
                        String action) {
        this.username = username;
        this.studentName = studentName;
        this.computerCode = computerCode;
        this.paperId = paperId;
        this.paperTitle = paperTitle;
        this.subject = subject;
        this.branch = branch;
        this.semester = semester;
        this.year = year;
        this.viewedAt = new Date();
        if (action != null && !action.isEmpty()) {
            this.action = action;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getComputerCode() {
        return computerCode;
    }

    public void setComputerCode(String computerCode) {
        this.computerCode = computerCode;
    }

    public Long getPaperId() {
        return paperId;
    }

    public void setPaperId(Long paperId) {
        this.paperId = paperId;
    }

    public String getPaperTitle() {
        return paperTitle;
    }

    public void setPaperTitle(String paperTitle) {
        this.paperTitle = paperTitle;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public int getSemester() {
        return semester;
    }

    public void setSemester(int semester) {
        this.semester = semester;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public Date getViewedAt() {
        return viewedAt;
    }

    public void setViewedAt(Date viewedAt) {
        this.viewedAt = viewedAt;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}
