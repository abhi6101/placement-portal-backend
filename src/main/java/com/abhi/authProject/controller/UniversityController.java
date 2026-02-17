package com.abhi.authProject.controller;

import com.abhi.authProject.model.University;
import com.abhi.authProject.repo.UniversityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/universities")
public class UniversityController {

    @Autowired
    private UniversityRepository universityRepository;

    @GetMapping
    public List<University> getAll() {
        return universityRepository.findAll();
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('DEPT_ADMIN')")
    @PostMapping
    public ResponseEntity<?> add(@RequestBody University university) {
        if (universityRepository.existsByName(university.getName())) {
            return ResponseEntity.badRequest().body("University already exists");
        }
        return ResponseEntity.ok(universityRepository.save(university));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('DEPT_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!universityRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        universityRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
