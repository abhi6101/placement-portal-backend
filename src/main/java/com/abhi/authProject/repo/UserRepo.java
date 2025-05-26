package com.abhi.authProject.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.abhi.authProject.model.Users;

@Repository
public interface UserRepo extends JpaRepository<Users,Integer> {


    Users findByUsername(String username);

    Object findByEmail(String email);
}
