package com.abhi.authProject.controller;

import com.abhi.authProject.model.Department;
import com.abhi.authProject.repo.DepartmentRepo;
import com.abhi.authProject.model.LeaderboardDto;
import com.abhi.authProject.model.Users;
import com.abhi.authProject.repo.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/public")
@CrossOrigin
public class PublicDataController {

    @Autowired
    private DepartmentRepo departmentRepo;

    @Autowired
    private UserRepo userRepo;

    @GetMapping("/departments")
    public ResponseEntity<List<Department>> getDepartments() {
        return ResponseEntity.ok(departmentRepo.findAll());
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<List<LeaderboardDto>> getLeaderboard() {
        List<Users> topUsers = userRepo.findTop10ByOrderByContributionPointsDesc();
        List<LeaderboardDto> dtoList = topUsers.stream()
                .filter(u -> u.getContributionPoints() != null && u.getContributionPoints() > 0)
                .map(u -> new LeaderboardDto(
                        u.getId(), 
                        u.getUsername(), 
                        u.getName() != null && !u.getName().isEmpty() ? u.getName() : u.getUsername(), 
                        u.getContributionPoints()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtoList);
    }
}
