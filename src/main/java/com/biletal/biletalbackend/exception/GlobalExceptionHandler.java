package com.biletal.biletalbackend.exception;

import com.biletal.biletalbackend.dto.ApiResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponseDto> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        // Log the 403 error with relevant information
        String requestURI = request.getDescription(false);
        
        Map<String, Object> details = new HashMap<>();
        details.put("timestamp", LocalDateTime.now().toString());
        details.put("uri", requestURI);
        details.put("message", ex.getMessage());
        
        logger.error("Access denied error: {}", details);
        
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ApiResponseDto("Bu işlem için yetkiniz bulunmamaktadır.", false));
    }
}
