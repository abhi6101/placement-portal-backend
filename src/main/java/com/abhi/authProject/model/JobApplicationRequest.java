package com.abhi.authProject.model;



import lombok.Data; // Assuming you use Lombok for boilerplate code

@Data // Generates getters, setters, toString, equals, hashCode
public class JobApplicationRequest {
    private String jobTitle;
    private String companyName;
    private String applicantName;
    private String applicantEmail;
    private String applicantRollNo;
    private String applicantPhone;
    // Add any other details you want to send to the admin
}