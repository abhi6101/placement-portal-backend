package com.abhi.authProject.controller;

import com.abhi.authProject.model.Student;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
public class StudentController {

    private List<Student> students =new ArrayList<>(List.of(
            new Student(1,"abhi",100),
            new Student(2,"Rahul",90),
            new Student(3,"Rohit",80)
    ));
    @GetMapping("/students")
    public List<Student> getStudents(){
        return students;
    }

    //with this method we can get CSRF token which is necessary for put,post,delete methods in spring security
    @GetMapping("/csrf-token")
    public CsrfToken getCsrfToken(HttpServletRequest request){
        return (CsrfToken) request.getAttribute("_csrf");
    }

    @PostMapping("/students")
    public Student addStudent(@RequestBody Student student){
        students.add(student);
        return student;
    }
}
