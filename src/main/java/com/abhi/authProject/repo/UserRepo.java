package com.abhi.authProject.repo;

import com.abhi.authProject.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional; // Import for Optional

@Repository
public interface UserRepo extends JpaRepository<Users, Integer> {

    // Change return type to Optional<Users> for better null handling
    Optional<Users> findByUsername(String username);

    // Change return type to Optional<Users> for better null handling
    Optional<Users> findByEmail(String email);

    // NEW: Method to find a user by their verification token
    Optional<Users> findByVerificationToken(String verificationToken);

    // NEW: Find students by branch and semester (for semester updates)
    List<Users> findByBranchAndSemester(String branch, Integer semester);

    // NEW: Find users by role and branch (for DEPT_ADMIN validation)
    List<Users> findByRoleAndBranch(String role, String branch);

    // NEW: Find users by role and admin branch (for DEPT_ADMIN validation)
    List<Users> findByRoleAndAdminBranch(String role, String adminBranch);

    // NEW: Find user by computer code (unique identifier like 59500)
    Optional<Users> findByComputerCode(String computerCode);

    // NEW: Find user by computer code OR username (for flexible login)
    default Optional<Users> findByComputerCodeOrUsername(String identifier) {
        return findByComputerCode(identifier)
                .or(() -> findByUsername(identifier));
    }
}
