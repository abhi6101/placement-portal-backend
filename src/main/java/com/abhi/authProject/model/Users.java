package com.abhi.authProject.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class Users {


    // @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "jobdetails_seq")
    // @SequenceGenerator(name = "jobdetails_seq", sequenceName = "jobdetails_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private int id;
    private String username;
    private String password;
    private String email;
    private String role;

}
