package com.abhi.authProject.model;

// Using Lombok for simplicity, or generate getters/setters manually
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {
    private int id;
    private String username;
    private String email;
    private String role;
    private boolean isVerified;
    private String companyName;
    private boolean enabled; // For company enable/disable feature
}