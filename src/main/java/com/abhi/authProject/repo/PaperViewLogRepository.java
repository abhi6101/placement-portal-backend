package com.abhi.authProject.repo;

import com.abhi.authProject.model.PaperViewLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PaperViewLogRepository extends JpaRepository<PaperViewLog, Long> {
    List<PaperViewLog> findAllByOrderByViewedAtDesc();
}
