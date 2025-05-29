package com.abhi.authProject.model;

import jakarta.validation.constraints.*;
import lombok.Data;


@Data
public class ResumeData {

    @NotBlank(message = "Name cannot be blank")
    @Size(max = 100, message = "Name cannot exceed 100 characters")
    private String name;

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email cannot exceed 100 characters")
    private String email;

    @NotBlank(message = "Phone cannot be blank")
    @Pattern(regexp = "^\\+?[0-9\\-()\\s]{7,20}$", message = "Invalid phone number format")
    private String phone;

    @Size(max = 255, message = "Address cannot exceed 255 characters")
    private String address;

    @Size(max = 255, message = "Permanent Address cannot exceed 255 characters")
    private String permanentAddress;

    @Size(max = 50, message = "Date of Birth cannot exceed 50 characters")
    private String dateOfBirth;

    @Pattern(regexp = "^(https?://)?(www\\.)?linkedin\\.com/in/[a-zA-Z0-9\\-_]+/?$", 
             message = "Invalid LinkedIn URL format")
    @Size(max = 255, message = "LinkedIn URL cannot exceed 255 characters")
    private String linkedin;

    @NotBlank(message = "Objective cannot be blank")
    @Size(max = 1000, message = "Objective section too long (max 1000 characters)")
    private String objective;

    @NotBlank(message = "Education cannot be blank")
    @Size(max = 2000, message = "Education section too long (max 2000 characters)")
    private String education;

    @Size(max = 4000, message = "Experience section too long (max 4000 characters)")
    private String experience;

    @NotBlank(message = "Skills cannot be blank")
    @Size(max = 1000, message = "Skills section too long (max 1000 characters)")
    private String skills;

    @Size(max = 3000, message = "Projects section too long (max 3000 characters)")
    private String projects;

    @Size(max = 100, message = "Languages known cannot exceed 100 characters")
    private String languages;

    @Size(max = 1000, message = "Hobbies/Interest section too long (max 1000 characters)")
    private String hobbies;

    @NotBlank(message = "Declaration cannot be blank")
    @Size(max = 1000, message = "Declaration section too long (max 1000 characters)")
    private String declaration;

    @NotBlank(message = "Template must be selected")
    @Pattern(regexp = "classic|modern|creative", message = "Invalid template selection")
    private String template;
}