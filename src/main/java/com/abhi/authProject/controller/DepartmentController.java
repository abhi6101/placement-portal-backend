package com.abhi.authProject.controller;

import com.abhi.authProject.model.*;
import com.abhi.authProject.repo.DepartmentBranchRepo;
import com.abhi.authProject.repo.DepartmentRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/departments")
@CrossOrigin
public class DepartmentController {

    @Autowired
    private DepartmentRepo departmentRepo;

    @Autowired
    private DepartmentBranchRepo branchRepo;

    // Get All Departments with Branches
    @GetMapping
    public ResponseEntity<List<DepartmentWithBranchesDto>> getAllDepartments() {
        List<Department> departments = departmentRepo.findAll();
        List<DepartmentWithBranchesDto> dtos = departments.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    // Get Single Department with Branches
    @GetMapping("/{id}")
    public ResponseEntity<DepartmentWithBranchesDto> getDepartment(@PathVariable Long id) {
        return departmentRepo.findById(id)
                .map(dept -> ResponseEntity.ok(convertToDto(dept)))
                .orElse(ResponseEntity.notFound().build());
    }

    // Add New Department with Branches (Super Admin Only)
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> addDepartment(@RequestBody DepartmentWithBranchesDto dto) {
        if (departmentRepo.existsByCode(dto.getCode())) {
            return ResponseEntity.badRequest().body("Department code already exists!");
        }

        Department department = new Department();
        department.setName(dto.getName());
        department.setCode(dto.getCode());
        department.setCategory(dto.getCategory());
        department.setHodName(dto.getHodName());
        department.setContactEmail(dto.getContactEmail());
        department.setDescription(dto.getDescription());

        // Save department first
        Department savedDept = departmentRepo.save(department);

        // Add branches if provided
        if (dto.getBranches() != null && !dto.getBranches().isEmpty()) {
            for (BranchDto branchDto : dto.getBranches()) {
                DepartmentBranch branch = new DepartmentBranch();
                branch.setDepartment(savedDept);
                branch.setBranchName(branchDto.getBranchName());
                branch.setBranchCode(branchDto.getBranchCode());
                branch.setDegree(branchDto.getDegree());
                branch.setMaxSemesters(branchDto.getMaxSemesters());
                branch.setHodName(branchDto.getHodName());
                branch.setContactEmail(branchDto.getContactEmail());
                branch.setDescription(branchDto.getDescription());
                branchRepo.save(branch);
            }
        }

        return ResponseEntity.ok(convertToDto(departmentRepo.findById(savedDept.getId()).get()));
    }

    // Add Branch to Existing Department
    @PostMapping("/{deptId}/branches")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> addBranch(@PathVariable Long deptId, @RequestBody BranchDto branchDto) {
        return departmentRepo.findById(deptId)
                .map(dept -> {
                    if (branchRepo.findByBranchCode(branchDto.getBranchCode()).isPresent()) {
                        return ResponseEntity.badRequest().body("Branch code already exists!");
                    }

                    DepartmentBranch branch = new DepartmentBranch();
                    branch.setDepartment(dept);
                    branch.setBranchName(branchDto.getBranchName());
                    branch.setBranchCode(branchDto.getBranchCode());
                    branch.setDegree(branchDto.getDegree());
                    branch.setMaxSemesters(branchDto.getMaxSemesters());
                    branch.setHodName(branchDto.getHodName());
                    branch.setContactEmail(branchDto.getContactEmail());
                    branch.setDescription(branchDto.getDescription());

                    DepartmentBranch saved = branchRepo.save(branch);
                    return ResponseEntity.ok(convertBranchToDto(saved));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Get All Branches (Flat list)
    @GetMapping("/branches")
    public ResponseEntity<List<BranchDto>> getAllBranches() {
        List<DepartmentBranch> branches = branchRepo.findAll();
        List<BranchDto> dtos = branches.stream()
                .map(this::convertBranchToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    // Update Department
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> updateDepartment(@PathVariable Long id, @RequestBody DepartmentWithBranchesDto dto) {
        return departmentRepo.findById(id)
                .map(dept -> {
                    dept.setName(dto.getName());
                    dept.setCode(dto.getCode());
                    dept.setCategory(dto.getCategory());
                    dept.setHodName(dto.getHodName());
                    dept.setContactEmail(dto.getContactEmail());
                    dept.setDescription(dto.getDescription());
                    Department saved = departmentRepo.save(dept);
                    return ResponseEntity.ok(convertToDto(saved));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Delete Department (cascades to branches)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> deleteDepartment(@PathVariable Long id) {
        if (!departmentRepo.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        departmentRepo.deleteById(id);
        return ResponseEntity.ok("Department and all branches deleted successfully.");
    }

    // Delete Branch
    @DeleteMapping("/branches/{branchId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> deleteBranch(@PathVariable Long branchId) {
        if (!branchRepo.existsById(branchId)) {
            return ResponseEntity.notFound().build();
        }
        branchRepo.deleteById(branchId);
        return ResponseEntity.ok("Branch deleted successfully.");
    }

    // Helper: Convert Department to DTO
    private DepartmentWithBranchesDto convertToDto(Department dept) {
        List<BranchDto> branchDtos = dept.getBranches() != null
                ? dept.getBranches().stream()
                        .map(this::convertBranchToDto)
                        .collect(Collectors.toList())
                : List.of();

        return new DepartmentWithBranchesDto(
                dept.getId(),
                dept.getName(),
                dept.getCode(),
                dept.getCategory(),
                dept.getHodName(),
                dept.getContactEmail(),
                dept.getDescription(),
                branchDtos);
    }

    // Helper: Convert Branch to DTO
    private BranchDto convertBranchToDto(DepartmentBranch branch) {
        return new BranchDto(
                branch.getId(),
                branch.getBranchName(),
                branch.getBranchCode(),
                branch.getDegree(),
                branch.getMaxSemesters(),
                branch.getHodName(),
                branch.getContactEmail(),
                branch.getDescription());
    }
}
