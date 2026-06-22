package com.abhi.authProject.Jwt;

import com.abhi.authProject.service.MyUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {

    @Autowired
    private JWTService jwtService;

    @Autowired
    private ApplicationContext context;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        String token = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        } else {
            token = request.getParameter("token");
        }

        // Log the token status for debugging
        System.out.println("Authorization Status - Header: " + (authHeader != null) + ", Query Token: " + (request.getParameter("token") != null));

        if (token != null && !token.trim().isEmpty()) {
                try {
                    String username = jwtService.extractUserName(token);

                    if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        MyUserDetailsService myUserDetailsService = context.getBean(MyUserDetailsService.class);
                        UserDetails userDetails = myUserDetailsService.loadUserByUsername(username);

                        if (jwtService.validateToken(token, userDetails)) {
                            UsernamePasswordAuthenticationToken authToken =
                                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            SecurityContextHolder.getContext().setAuthentication(authToken);
                        }
                    }
                } catch (Exception e) {
                    // Log and ignore invalid token errors
                    System.err.println("Invalid JWT token: " + e.getMessage());
                }
        }

        filterChain.doFilter(request, response);
    }
}
