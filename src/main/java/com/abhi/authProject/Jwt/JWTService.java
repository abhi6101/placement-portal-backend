package com.abhi.authProject.Jwt;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;


import com.abhi.authProject.model.BlackListedToken;
import com.abhi.authProject.repo.BlacklistedTokenRepository;
import com.abhi.authProject.service.MyUserDetailsService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
public class JWTService {

    @Value("${jwt.secret}")
    private String secretkey;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private BlacklistedTokenRepository blacklistedTokenRepository;

    

    // Scheduled task to clean expired blacklisted tokens every 24 hours
    @Scheduled(fixedRate = 86400000)
    public void cleanExpiredBlacklistedTokens() {
        blacklistedTokenRepository.deleteByExpiryDateBefore(new Date());
    }

    public String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        UserDetails userDetails = context.getBean(MyUserDetailsService.class)
                                         .loadUserByUsername(username);
                                         claims.put("authorities", userDetails.getAuthorities()
                                         .stream()
                                         .map(auth -> "ROLE_" + auth.getAuthority().replace("ROLE_", ""))
                                         .collect(Collectors.toList()));
                                     
    
        return Jwts.builder()
                   .claims(claims)
                   .subject(username)
                   .issuedAt(new Date(System.currentTimeMillis()))
                   .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10)) // 10 hours
                   .signWith(getKey(), Jwts.SIG.HS384)
                   .compact();
    }

   private SecretKey getKey() {
    byte[] keyBytes = secretkey.getBytes(StandardCharsets.UTF_8); // No Base64 decoding
    return Keys.hmacShaKeyFor(keyBytes);
}

    public String extractUserName(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimResolver) {
        final Claims claims = extractAllClaims(token);
        return claimResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                   .verifyWith(getKey())
                   .build()
                   .parseSignedClaims(token)
                   .getPayload();
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        final String userName = extractUserName(token);
        return (userName.equals(userDetails.getUsername())
                && !isTokenExpired(token)
                && !isTokenBlacklisted(token));
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public boolean isTokenBlacklisted(String token) {
        return blacklistedTokenRepository.existsById(token);
    }

    public void forceBlacklistToken(String token) {
        if (!isTokenBlacklisted(token)) {
            BlackListedToken blacklistedToken = new BlackListedToken();
            blacklistedToken.setToken(token);
            blacklistedToken.setExpiryDate(new Date(System.currentTimeMillis() + 86400000)); // 24 hours
            blacklistedTokenRepository.save(blacklistedToken);
        }
    }

    public void blacklistToken(String token) {
        try {
            if (!isTokenBlacklisted(token)) {
                BlackListedToken blacklistedToken = new BlackListedToken();
                blacklistedToken.setToken(token);

                Date expiry = extractExpirationSafe(token);
                blacklistedToken.setExpiryDate(expiry != null ? expiry :
                        new Date(System.currentTimeMillis() + 86400000));

                blacklistedTokenRepository.save(blacklistedToken);
            }
        } catch (Exception e) {
            System.err.println("Error blacklisting token: " + e.getMessage());
        }
    }

    private Date extractExpirationSafe(String token) {
        try {
            return extractExpiration(token);
        } catch (Exception e) {
            return null;
        }
    }
}
