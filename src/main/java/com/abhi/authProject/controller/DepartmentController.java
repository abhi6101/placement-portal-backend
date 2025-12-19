package com.abhi.authProject.controller;

import com.abhi.authProject.model.Department;
import com.abhi.authProject.repo.DepartmentRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/admin/departments")
@CrossOrigin // CORS for frontend
public class DepartmentController {

    @Autowired
    private DepartmentRepo departmentRepo;

    // Get All Departments (Public/Authenticated)
    @GetMapping
    public ResponseEntity<List<Department>> getAllDepartments() {
        return ResponseEntity.ok(departmentRepo.findAll());
    }

    // Add New Department (Super Admin Only)
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> addDepartment(@RequestBody Department department) {
        if (departmentRepo.existsByCode(department.getCode())) {
            return ResponseEntity.badRequest().body("Department code already exists!");
        }
        return ResponseEntity.ok(departmentRepo.save(department));
    }

    // Initialize Default Departments (One-time utility)
    @PostMapping("/init-defaults")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> initDefaults() {
        List<Department> defaults = Arrays.asList(
                new Department(null, "Integrated Master of Computer Applications", "IMCA", "Head IMCA",
                        "imca@college.edu"),
                new Department(null, "Master of Computer Applications", "MCA", "Head MCA", "mca@college.edu"),
                new Department(null, "Bachelor of Computer Applications", "BCA", "Head BCA", "bca@college.edu"));

        int count = 0;
        for (Department d : defaults) {
            if (!departmentRepo.existsByCode(d.getCode())) {
                departmentRepo.save(d);
                count++;
            }
        }
        return ResponseEntity.ok("Added " + count + " new default departments.");
    }

    // Delete Department (Super Admin Only)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> deleteDepartment(@PathVariable Long id) {
        if (!departmentRepo.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        departmentRepo.deleteById(id);
        return ResponseEntity.ok("Department deleted successfully.");
    }
}
