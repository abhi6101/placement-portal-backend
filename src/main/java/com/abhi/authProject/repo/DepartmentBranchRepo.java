package com.abhi.authProject.repo;

import com.abhi.authProject.model.DepartmentBranch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentBranchRepo extends JpaRepository<DepartmentBranch, Long> {

    // Find branch by code
    Optional<DepartmentBranch> findByBranchCode(String branchCode);

    // Find all branches under a department
    List<DepartmentBranch> findByDepartmentId(Long departmentId);

    // Find branch by name
    Optional<DepartmentBranch> findByBranchName(String branchName);
}
