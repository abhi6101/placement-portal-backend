package com.abhi.authProject.config;

import com.abhi.authProject.Jwt.JwtFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException; // Import DisabledException
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private JwtFilter jwtFilter; // Assuming you have a JwtFilter class

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable) // Disable CSRF for stateless REST APIs
            .cors(cors -> cors.configurationSource(corsConfigurationSource())) // Enable CORS
            .authorizeHttpRequests(auth -> auth
                // Permit ALL requests to your public API endpoints
                .requestMatchers(HttpMethod.POST,
                    "/api/auth/register",
                    "/api/auth/login",
                    "/api/auth/verify-code",
                    "/api/auth/forgot-password", // Endpoint to request a password reset link
                    "/api/auth/reset-password"   // Endpoint to submit the new password with token
                ).permitAll()
                .requestMatchers(HttpMethod.GET,
                    "/api/auth/forgot-password", // If you have a GET endpoint for forgot password, permit it.
                    "/api/auth/reset-password",  // If you have a GET endpoint for reset password, permit it.
                                                 // Note: Your frontend will send a POST to /api/auth/reset-password,
                                                 // but a GET might be needed for initial token validation or similar.
                    "/api/papers" // Your public papers API
                    // Add any other truly public GET API endpoints here
                ).permitAll()

                // Secure your admin and user specific API endpoints
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/user/**").hasRole("USER")
                .requestMatchers(HttpMethod.POST, "/api/apply-job").hasRole("USER")

                // All other API requests require authentication
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, e) -> {
                    // Handle DisabledException for unverified accounts
                    if (e instanceof DisabledException) {
                        res.setStatus(HttpStatus.FORBIDDEN.value()); // Or HttpStatus.UNAUTHORIZED
                        res.getWriter().write(e.getMessage());
                    }
                    // Handle BadCredentialsException for invalid username/password
                    else if (e instanceof BadCredentialsException) {
                        res.setStatus(HttpStatus.UNAUTHORIZED.value());
                        res.getWriter().write("Invalid username or password");
                    }
                    // Handle other unauthorized access attempts
                    else {
                        res.setStatus(HttpStatus.UNAUTHORIZED.value());
                        res.getWriter().write("Unauthorized: Please login first");
                    }
                })
                .accessDeniedHandler((req, res, e) -> {
                    // Handle forbidden access (authenticated but insufficient role)
                    res.setStatus(HttpStatus.FORBIDDEN.value());
                    res.getWriter().write("Forbidden: You don't have permission");
                })
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // Use stateless sessions for JWT
            )
            .authenticationProvider(authenticationProvider()); // Set custom authentication provider

        // Add the JWT filter before Spring Security's default username/password authentication filter
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // IMPORTANT: Add your actual deployed frontend URL(s) here.
        // If your frontend is deployed on Render at e.g., "https://hack-2-hired.onrender.com", add it.
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:5500",    // For local frontend development
            "http://127.0.0.1:5500",    // For local frontend development
            // "https://placement-portal-backend-nwaj.onrender.com", // REMOVE this if it's not your frontend!
            "https://YOUR-ACTUAL-FRONTEND-URL.onrender.com" // <--- REPLACE THIS WITH YOUR DEPLOYED FRONTEND URL
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*")); // Allow all headers
        configuration.setAllowCredentials(true); // Allow sending of cookies/authorization headers

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // Apply this CORS config to all paths
        return source;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setPasswordEncoder(bCryptPasswordEncoder()); // Set the password encoder
        provider.setUserDetailsService(userDetailsService); // Set your custom UserDetailsService
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager(); // Expose AuthenticationManager
    }
}