package com.biletal.biletalbackend.controller;

import com.biletal.biletalbackend.dto.ApiResponseDto;
import com.biletal.biletalbackend.dto.UserUpdateRequest;
import com.biletal.biletalbackend.security.JwtService;
import com.biletal.biletalbackend.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User Management", description = "APIs for user management")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;
    private final JwtService jwtService;

    @Autowired
    public UserController(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    /**
     * Endpoint for authenticated end users to delete their own account
     * Performs a soft delete by setting a flag in the database
     */
    @Operation(
        summary = "Delete user account", 
        description = "Deletes the authenticated user's account (soft delete)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Account deleted successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteAccount(HttpServletRequest request) {
        // Extract token from the Authorization header
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        
        if (header == null || !header.startsWith("Bearer ")) {
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponseDto("Yetkilendirme hatası: Token bulunamadı.", false));
        }
        
        String token = header.substring(7); // Remove "Bearer " prefix
        
        try {
            // Extract user ID from token
            Long userId = jwtService.extractClaim(token, claims -> claims.get("userId", Long.class));
            String role = jwtService.extractClaim(token, claims -> claims.get("role", String.class));
            
            if (role == null || !role.equals("END_USER")) {
                return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponseDto("Yetkisiz erişim: Bu işlem sadece son kullanıcılar için geçerlidir.", false));
            }

            if (userId == null) {
                return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponseDto("Geçersiz token: Kullanıcı bilgisi bulunamadı.", false));
            }
            
            // Delete the user account (soft delete)
            userService.deleteUserAccount(userId);
            
            return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ApiResponseDto("Hesabınız başarıyla silindi.", true));
            
        } catch (NoSuchElementException e) {
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ApiResponseDto("Kullanıcı bulunamadı.", false));
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDto("Hesap silinirken bir hata oluştu.", false));
        }
    }
    
    /**
     * Endpoint for authenticated end users to update their profile information
     */
    @Operation(
        summary = "Update user profile", 
        description = "Updates the authenticated user's profile information"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Profile updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - not an end user"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PutMapping("/update")
    public ResponseEntity<?> updateProfile(@Valid @RequestBody UserUpdateRequest request, HttpServletRequest httpRequest) {
        // Extract token from the Authorization header
        String header = httpRequest.getHeader(HttpHeaders.AUTHORIZATION);
        
        if (header == null || !header.startsWith("Bearer ")) {
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponseDto("Yetkilendirme hatası: Token bulunamadı.", false));
        }
        
        String token = header.substring(7); // Remove "Bearer " prefix
        
        try {
            // Extract user ID and role from token
            Long userId = jwtService.extractClaim(token, claims -> claims.get("userId", Long.class));
            
            if (userId == null) {
                return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponseDto("Geçersiz token: Kullanıcı bilgisi bulunamadı.", false));
            }
            
            // Update the user profile
            userService.updateUserProfile(userId, request);
            
            return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ApiResponseDto("Profil bilgileriniz başarıyla güncellendi.", true));
                
        } catch (NoSuchElementException e) {
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ApiResponseDto("Kullanıcı bulunamadı.", false));
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponseDto("Geçersiz bilgi girdiniz: " + e.getMessage(), false));
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDto("Güncelleme sırasında bir hata oluştu.", false));
        }
    }


}