package com.abhi.authProject.repo;



import java.util.Date;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import com.abhi.authProject.model.BlackListedToken;

public interface BlacklistedTokenRepository extends JpaRepository<BlackListedToken , String> {
  boolean existsByToken(String token);

  @Modifying
    @Query("DELETE FROM BlackListedToken t WHERE t.expiryDate < :now")
    void deleteByExpiryDateBefore(@Param("now") Date now);
}
