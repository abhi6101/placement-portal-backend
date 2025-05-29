package com.abhi.authProject.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Student {


    private int id;
    private String name;
    private int marks;
//With lombok we don't have to create all of this boilerplate code

}
