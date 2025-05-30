package com.biletal.biletalbackend.config;

import com.biletal.biletalbackend.security.JwtTokenFilter;
import com.biletal.biletalbackend.security.SecurityLoggingFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtTokenFilter jwtTokenFilter;
    private final SecurityLoggingFilter securityLoggingFilter;
    
    private static final String[] AUTH_WHITELIST = {
        // -- Swagger UI v3 (OpenAPI)
        "/v3/api-docs",
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/swagger-resources/**",
        "/swagger-resources",
        "/configuration/ui",
        "/configuration/security",
        "/webjars/**",
        "/favicon.ico",
        "/logo/**",
        // -- Public endpoints
        "/activate",
        "/api/users/delete",
        "/api/auth/current-user",
        "/api/auth/register",
        "/api/auth/login",
        "/api/auth/admin/login",
        "/api/set-password",
        "/api/logout",
        "/api/auth/forgot-password",
        "/api/auth/reset-password",
        "/login",
        "/reset-password",
        "/",
        // -- Flight public endpoints (GET operations)
        "/api/flights",
        "/api/flights/search",
        "/api/flights/available", 
        "/api/flights/paginated",
        "/api/flights/search/all",
        "/api/flights/route",
        "/api/flights/airline", 
        "/api/flights/time-range",
        // -- Bus Expedition public endpoints (GET operations)
        "/api/bus-expeditions",
        "/api/bus-expeditions/search",
        "/api/bus-expeditions/available", 
        "/api/bus-expeditions/paginated",
        "/api/bus-expeditions/search/all",
        "/api/bus-expeditions/route",
        "/api/bus-expeditions/company", 
        "/api/bus-expeditions/time-range",
        // -- H2 Console (for testing)
        "/h2-console/**"
    };
    
    public SecurityConfig(JwtTokenFilter jwtTokenFilter, SecurityLoggingFilter securityLoggingFilter) {
        this.jwtTokenFilter = jwtTokenFilter;
        this.securityLoggingFilter = securityLoggingFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.disable())) // H2 Console için
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(AUTH_WHITELIST).permitAll()
                .requestMatchers(HttpMethod.GET, "/api/flights/*").permitAll() // Allow GET requests to individual flights
                .requestMatchers(HttpMethod.GET, "/api/bus-expeditions/*").permitAll() // Allow GET requests to individual bus expeditions
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            );

        // JWT filtresi sadece korumalı endpointlerde çalışsın
        http.addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(securityLoggingFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
    
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("authorization", "content-type", "x-auth-token"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
