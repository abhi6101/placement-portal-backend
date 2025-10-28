package com.abhi.authProject.controller;

import com.abhi.authProject.model.UserDto; // Import the new DTO
import com.abhi.authProject.model.Users;
import com.abhi.authProject.repo.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors; // Import collectors

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    @Autowired
    private UserRepo userRepo;

    @GetMapping("/users")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<Users> users = userRepo.findAll();

        // CORRECTED: Map the Users entity to the UserDto to exclude sensitive data
        List<UserDto> userDtos = users.stream()
            .map(user -> new UserDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.isVerified()
            ))
            .collect(Collectors.toList());

        return ResponseEntity.ok(userDtos);
    }
}