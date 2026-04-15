package com.abhi.authProject.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@RestController

public class HomeController {

    @GetMapping("/index")
    public String home(HttpServletRequest request) {
      return "Welcome To placement Portal" ;
}


}

