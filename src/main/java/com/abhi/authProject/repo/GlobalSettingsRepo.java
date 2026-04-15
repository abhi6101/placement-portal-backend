package com.abhi.authProject.repo;

import com.abhi.authProject.model.GlobalSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GlobalSettingsRepo extends JpaRepository<GlobalSettings, Long> {
}
