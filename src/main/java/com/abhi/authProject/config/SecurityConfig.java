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
@EnableMethodSecurity // Keep this enabled for @PreAuthorize
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
            .csrf(AbstractHttpConfigurer::disable) // Disable CSRF for stateless REST APIs
            .cors(cors -> cors.configurationSource(corsConfigurationSource())) // Enable CORS with custom source
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET,
                    "/", "/home", "/index", "/login.html", "/register.html", "/verify-account.html",
                    "/css/**", "/js/**", "/images/**", "/jobs", "/api/resume/download/**",
                    "/error.html",
                    "/papers.html", "/papers.css", "/papers.js",
                    // --- NEW: Permit access to Forgot Password and Reset Password HTML/CSS/JS if they are served directly ---
                    "/forgot-password.html", "/reset-password.html",
                    "/forgot-password.css", "/forgot-password.js", // If you create separate files
                    "/reset-password.css", "/reset-password.js"     // If you create separate files
                ).permitAll()
                .requestMatchers(HttpMethod.POST,
                    "/api/auth/register",
                    "/api/auth/login",
                    "/api/auth/verify-code",
                    "/api/auth/logout",
                    "/api/resume/generate-pdf",
                    // --- NEW: Permit access to Forgot Password and Reset Password API endpoints ---
                    "/api/auth/forgot-password", // Endpoint to request a password reset link
                    "/api/auth/reset-password"   // Endpoint to submit the new password with token
                ).permitAll()

                // Existing rules for /api/papers
                .requestMatchers(HttpMethod.GET, "/api/papers").permitAll() // Example: Public access

                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/user/**").hasRole("USER")
                .requestMatchers(HttpMethod.POST, "/api/apply-job").hasRole("USER")
                .anyRequest().authenticated() // All other requests require authentication
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
                        res.getWriter().write("Unauthorized: Please login first");
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
            "http://localhost:5500",
            "http://127.0.0.1:5500",
            "https://hack-2-hired.onrender.com" // Your deployed frontend URL
            // Add any other specific frontend origins as needed
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