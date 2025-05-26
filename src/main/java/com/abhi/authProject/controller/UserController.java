package com.abhi.authProject.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.abhi.authProject.Jwt.JWTService;
import com.abhi.authProject.model.Users;
import com.abhi.authProject.service.UserService;

import jakarta.servlet.http.HttpServletResponse;

@RequestMapping("/api/auth")
@RestController
public class UserController {

    @Autowired
    private UserService service;
    @Autowired
    private JWTService jwtService ;

    // @PostMapping("/register")
    // public Users reqister(@RequestBody Users user){
    //     return service.register(user);
    // }

@PostMapping("/register")
public ResponseEntity<Map<String, String>> register(@RequestBody Users user) {
    Map<String, String> response = new HashMap<>();
    try {
        Users registeredUser = service.register(user);
        response.put("status", "success");
        response.put("message", "Registration successful");
        return ResponseEntity.ok(response);
    } catch (Exception e) {
        response.put("status", "error");
        response.put("message", e.getMessage());
        return ResponseEntity.badRequest().body(response);
    }
}

@PostMapping("/login")
public ResponseEntity<Map<String, Object>> login(@RequestBody Users user) {
    Map<String, Object> response = new HashMap<>();
    try {
        String token = service.verify(user);
        response.put("status", "success");
        response.put("token", token);
        response.put("message", "Login successful");
        return ResponseEntity.ok()
                .header("Authorization", "Bearer " + token)
                .body(response);
    } catch (Exception e) {
        response.put("status", "error");
        response.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }
}
@PostMapping("/logout")
public ResponseEntity<String> logout(
    @RequestHeader(value = "Authorization", required = false) String authHeader,
    HttpServletResponse servletResponse
) {
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
        String token = authHeader.substring(7);
        // Use forceBlacklistToken instead of blacklistToken
        jwtService.forceBlacklistToken(token);
    }
    
    // Clear client-side token
    servletResponse.setHeader("Authorization", "");
    return ResponseEntity.ok("Logged out successfully");
}



}




