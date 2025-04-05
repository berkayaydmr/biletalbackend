package com.biletal.biletalbackend.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
public class SecurityLoggingFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(SecurityLoggingFilter.class);
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) 
            throws ServletException, IOException {
        
        StatusCapturingResponseWrapper responseWrapper = new StatusCapturingResponseWrapper(response);
        
        try {
            filterChain.doFilter(request, responseWrapper);
        } finally {
            int status = responseWrapper.getStatus();
            
            // Log all 403 Forbidden responses
            if (status == HttpServletResponse.SC_FORBIDDEN) {
                Map<String, Object> logDetails = new HashMap<>();
                logDetails.put("timestamp", LocalDateTime.now().toString());
                logDetails.put("status", status);
                logDetails.put("path", request.getRequestURI());
                logDetails.put("method", request.getMethod());
                logDetails.put("remote_addr", request.getRemoteAddr());
                
                // Try to get user information if available
                String authorization = request.getHeader("Authorization");
                if (authorization != null && authorization.startsWith("Bearer ")) {
                    logDetails.put("token_present", true);
                } else {
                    logDetails.put("token_present", false);
                }
                
                logger.warn("Forbidden access attempt: {}", logDetails);
            }
        }
    }
    
    // Wrapper class to capture the status code
    private static class StatusCapturingResponseWrapper extends HttpServletResponseWrapper {
        private int status;
        
        public StatusCapturingResponseWrapper(HttpServletResponse response) {
            super(response);
            this.status = 200; // Default status
        }
        
        @Override
        public void setStatus(int sc) {
            super.setStatus(sc);
            this.status = sc;
        }
        
        @Override
        public void sendError(int sc) throws IOException {
            super.sendError(sc);
            this.status = sc;
        }
        
        @Override
        public void sendError(int sc, String msg) throws IOException {
            super.sendError(sc, msg);
            this.status = sc;
        }
        
        public int getStatus() {
            return status;
        }
    }
}
