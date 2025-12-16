package com.abhi.authProject.controller;

import com.abhi.authProject.model.CompanyStatsDto;
import com.abhi.authProject.model.Users;
import com.abhi.authProject.repo.InterviewDriveRepo;
import com.abhi.authProject.repo.JobDetailsRepo;
import com.abhi.authProject.repo.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/stats")
@PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
public class AdminStatsController {

    @Autowired
    private JobDetailsRepo jobDetailsRepo;

    @Autowired
    private InterviewDriveRepo interviewDriveRepo;

    @Autowired
    private UserRepo userRepo;

    @GetMapping("/companies")
    public ResponseEntity<List<CompanyStatsDto>> getCompanyStats() {
        // 1. Gather all unique company names from Users (COMPANY_ADMINs)
        Set<String> companyNames = new HashSet<>();
        List<Users> companyAdmins = userRepo.findAll().stream()
                .filter(u -> "COMPANY_ADMIN".equals(u.getRole()) && u.getCompanyName() != null)
                .collect(Collectors.toList());

        for (Users admin : companyAdmins) {
            if (!admin.getCompanyName().isEmpty()) {
                companyNames.add(admin.getCompanyName());
            }
        }

        // 2. Also check JobDetails specifically to catch legacy jobs with company names
        // (Optional, but good for completeness)
        jobDetailsRepo.findAll().forEach(job -> {
            if (job.getCompany_name() != null && !job.getCompany_name().isEmpty()) {
                companyNames.add(job.getCompany_name());
            }
        });

        // 3. Build Stats DTOs
        List<CompanyStatsDto> statsList = new ArrayList<>();
        for (String company : companyNames) {
            long jobCount = jobDetailsRepo.countByCompany_name(company);
            long interviewCount = interviewDriveRepo.countByCompany(company);
            statsList.add(new CompanyStatsDto(company, jobCount, interviewCount));
        }

        return ResponseEntity.ok(statsList);
    }
}
