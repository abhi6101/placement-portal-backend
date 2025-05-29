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
    private JwtFilter jwtFilter;

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
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
                    "/api/auth/reset-password"   // If you have a GET endpoint for reset password, permit it.
                                                 // Note: Your frontend will send a POST to /api/auth/reset-password,
                                                 // but a GET might be needed for initial token validation or similar.
                    "/api/papers" // Your public papers API
                    // Add any other truly public GET API endpoints here
                ).permitAll()

                // Remove ALL frontend specific paths from here, as they are served by the Static Site.
                // For example, remove: "/", "/home", "/index", "/login.html", "/register.html",
                // "/verify-account.html", "/css/**", "/js/**", "/images/**", "/jobs",
                // "/error.html", "/papers.html", "/papers.css", "/papers.js",
                // "/forgot-password.html", "/reset-password.html",
                // "/forgot-password.css", "/forgot-password.js",
                // "/reset-password.css", "/reset-password.js"

                // Secure your admin and user specific API endpoints
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/user/**").hasRole("USER")
                .requestMatchers(HttpMethod.POST, "/api/apply-job").hasRole("USER")

                // All other API requests require authentication
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, e) -> {
                    if (e instanceof BadCredentialsException && e.getMessage().contains("Please verify your email address")) {
                        res.setStatus(HttpStatus.FORBIDDEN.value());
                        res.getWriter().write(e.getMessage());
                    } else if (e instanceof BadCredentialsException) {
                        res.setStatus(HttpStatus.UNAUTHORIZED.value());
                        res.getWriter().write("Invalid username or password");
                    } else {
                        res.setStatus(HttpStatus.UNAUTHORIZED.value());
                        res.getWriter().write("Unauthorized: Please login first"); //
                    }
                })
                .accessDeniedHandler((req, res, e) -> {
                    res.setStatus(HttpStatus.FORBIDDEN.value());
                    res.getWriter().write("Forbidden: You don't have permission");
                })
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authenticationProvider(authenticationProvider());

        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // IMPORTANT: Ensure your frontend's deployed Render URL is included here.
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:5500", // For local development
            "http://127.0.0.1:5500", // For local development
            "https://placement-portal-backend-nwaj.onrender.com" // Your deployed frontend URL
            // Add any other specific frontend origins if they access this backend
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setPasswordEncoder(bCryptPasswordEncoder());
        provider.setUserDetailsService(userDetailsService);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}