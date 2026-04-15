package com.abhi.authProject.repo;

import com.abhi.authProject.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DepartmentRepo extends JpaRepository<Department, Long> {
    Optional<Department> findByCode(String code);

    boolean existsByCode(String code);
}
