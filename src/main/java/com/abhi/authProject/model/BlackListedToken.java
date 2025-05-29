package com.abhi.authProject.model;


import java.util.Date;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "blacklisted_tokens")
public class BlackListedToken {
    @Id
    private String token;
    
    private Date expiryDate;
   // Properly implement setExpiryDate
   public void setExpiryDate(Date expiryDate) {
    this.expiryDate = expiryDate;
}
    
}
