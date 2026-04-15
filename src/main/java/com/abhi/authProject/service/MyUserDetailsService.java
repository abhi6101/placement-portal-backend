package com.abhi.authProject.service;

import com.abhi.authProject.model.Users;
import com.abhi.authProject.repo.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;

import org.springframework.security.core.userdetails.User; // This import is crucial for Spring Security's User class

@Service
public class MyUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepo repo;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Users user = repo.findByUsername(username)
                         .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        // --- START: CRITICAL CHANGE FOR VERIFICATION ENFORCEMENT ---
        // If the user is not verified, throw an exception to prevent login
        if (!user.isVerified()) {
            throw new UsernameNotFoundException("Account not verified. Please check your email for verification instructions.");
        }

        // Return Spring Security's User object, mapping your 'isVerified' to 'enabled'
        return new User(
            user.getUsername(),
            user.getPassword(),
            user.isVerified(),      // enabled: true if verified, false otherwise
            true,                   // accountNonExpired: Set to true unless you have specific expiration logic
            true,                   // credentialsNonExpired: Set to true unless you have password expiration logic
            true,                   // accountNonLocked: Set to true unless you have account locking logic
            getAuthorities(user.getRole()) // User's roles/authorities
        );
        // --- END: CRITICAL CHANGE FOR VERIFICATION ENFORCEMENT ---
    }

    private Collection<? extends GrantedAuthority> getAuthorities(String role) {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role));
    }
}