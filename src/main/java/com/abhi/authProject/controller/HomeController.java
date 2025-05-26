package com.abhi.authProject.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@CrossOrigin(origins = {"http://localhost:5500","http://127.0.0.1:5500"})
public class HomeController {

    @GetMapping("/index")
    public String home(HttpServletRequest request) {
      return "Welcome To placement Portal" ;
}

}
