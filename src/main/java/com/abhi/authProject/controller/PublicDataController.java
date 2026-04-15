package com.abhi.authProject.controller;

import com.abhi.authProject.model.Department;
import com.abhi.authProject.repo.DepartmentRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/public")
@CrossOrigin
public class PublicDataController {

    @Autowired
    private DepartmentRepo departmentRepo;

    @GetMapping("/departments")
    public ResponseEntity<List<Department>> getDepartments() {
        return ResponseEntity.ok(departmentRepo.findAll());
    }
}
