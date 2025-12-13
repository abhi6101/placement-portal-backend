package com.abhi.authProject.controller;

import com.abhi.authProject.model.InterviewDrive;
import com.abhi.authProject.repo.InterviewDriveRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@CrossOrigin
@RequestMapping("/api/interview-drives")
public class InterviewDriveController {

    @Autowired
    private InterviewDriveRepo interviewDriveRepo;

    @GetMapping
    public List<InterviewDrive> getAllDrives() {
        // Return only future/today drives for cleaner list, or all?
        // Let's return all upcoming for now
        return interviewDriveRepo.findByDateAfterOrderByDateAsc(LocalDate.now().minusDays(1));
    }

    @PostMapping("/admin")
    public InterviewDrive createDrive(@RequestBody InterviewDrive drive) {
        return interviewDriveRepo.save(drive);
    }

    @DeleteMapping("/admin/{id}")
    public ResponseEntity<?> deleteDrive(@PathVariable Long id) {
        interviewDriveRepo.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
