package com.biletal.biletalbackend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtTokenFilter extends OncePerRequestFilter {

    private final CustomUserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final TokenWhitelistService whitelistService;

    public JwtTokenFilter(
            CustomUserDetailsService userDetailsService,
            JwtService jwtService,
            TokenWhitelistService whitelistService) {
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.whitelistService = whitelistService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        
        // Skip JWT validation for public endpoints (including Swagger)
        String requestPath = request.getRequestURI();
        System.out.println("JWT Filter - Request Path: " + requestPath + ", Is Public: " + isPublicEndpoint(requestPath));
        
        if (isPublicEndpoint(requestPath)) {
            chain.doFilter(request, response);
            return;
        }
        
        // Get authorization header
        final String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        
        // Check if header is missing or doesn't start with "Bearer "
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }
        
        // Extract token
        final String token = header.substring(7);
        
        // Check if token is in whitelist
        if (!whitelistService.isTokenValid(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Token geçersiz veya oturum sona erdi");
            return;
        }
        
        // Validate token and set authentication
        try {
            String email = jwtService.extractUsername(token);
            
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                
                if (jwtService.validateToken(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authentication = 
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (Exception e) {
            // Token validation failed
        }
        
        chain.doFilter(request, response);
    }
    
    private boolean isPublicEndpoint(String requestPath) {
        return requestPath.equals("/v3/api-docs") ||
               requestPath.startsWith("/v3/api-docs/") ||
               requestPath.startsWith("/swagger-ui") ||
               requestPath.equals("/swagger-ui.html") ||
               requestPath.startsWith("/swagger-resources") ||
               requestPath.startsWith("/webjars") ||
               requestPath.startsWith("/configuration") ||
               requestPath.equals("/favicon.ico") ||
               requestPath.equals("/activate") ||
               requestPath.equals("/api/auth/register") ||
               requestPath.equals("/api/auth/login") ||
               requestPath.equals("/api/auth/admin/login") ||
               requestPath.equals("/api/set-password") ||
               requestPath.equals("/api/logout") ||
               requestPath.equals("/api/auth/forgot-password") ||
               requestPath.equals("/api/auth/reset-password") ||
               requestPath.equals("/login") ||
               requestPath.equals("/reset-password") ||
               requestPath.equals("/");
    }
}
