 package com.abhi.authProject.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.abhi.authProject.Jwt.JWTService;
import com.abhi.authProject.model.Users;
import com.abhi.authProject.repo.UserRepo;

// import com.abhi.authProject.Jwt.JWTService;
// import com.abhi.authProject.model.Users;
// import com.abhi.authProject.repo.UserRepo;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.security.authentication.AuthenticationManager;
// import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
// import org.springframework.security.core.Authentication;
// import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
// import org.springframework.stereotype.Service;

// @Service
// public class UserService {
//     @Autowired
//     private UserRepo repo;

//     @Autowired
//     private JWTService jwtservice;

//     @Autowired
//     private AuthenticationManager authManager;
//     private BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

//     public Users register(Users user){
//         user.setPassword(encoder.encode(user.getPassword()));
//         return repo.save(user);
//     }


//     //uses authManager to authenticate the user
//     public String verify(Users user) {
//         Authentication authentication  =
//                 authManager.authenticate(new UsernamePasswordAuthenticationToken(user.getUsername(),user.getPassword()));
//         if(authentication.isAuthenticated()){
//             return jwtservice.generateToken(user.getUsername());
//         }
//         else return "failed";

//     }
// }
@Service
public class UserService {
    @Autowired
    private UserRepo repo;

    @Autowired
    private JWTService jwtservice;

    @Autowired
    private AuthenticationManager authManager;

    @Autowired
    private BCryptPasswordEncoder encoder;

    public Users register(Users user) {
        // Check if username already exists
        if (repo.findByUsername(user.getUsername()) != null) {
            throw new RuntimeException("Username already exists");
        }
        
        // Check if email already exists
        if (repo.findByEmail(user.getEmail()) != null) {
            throw new RuntimeException("Email already registered");
        }
        
        // Encode password and save user
        user.setPassword(encoder.encode(user.getPassword()));
        return repo.save(user);
    }

    public String verify(Users user) {
        Authentication authentication = authManager.authenticate(
            new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword())
        );
        if (authentication.isAuthenticated()) {
            return jwtservice.generateToken(user.getUsername());
        }
        return "Authentication failed";
    }
}
