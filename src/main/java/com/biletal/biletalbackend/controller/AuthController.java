package com.biletal.biletalbackend.controller;

import com.biletal.biletalbackend.dto.AdminUpdateRequest;
import com.biletal.biletalbackend.dto.ApiResponseDto;
import com.biletal.biletalbackend.dto.ForgotPasswordRequest;
import com.biletal.biletalbackend.dto.LoginRequest;
import com.biletal.biletalbackend.dto.LoginResponse;
import com.biletal.biletalbackend.dto.PasswordRequest;
import com.biletal.biletalbackend.dto.RegistrationRequest;
import com.biletal.biletalbackend.dto.ResetPasswordRequest;
import com.biletal.biletalbackend.dto.UserResponseDto;
import com.biletal.biletalbackend.model.PasswordResetToken;
import com.biletal.biletalbackend.model.RegistrationToken;
import com.biletal.biletalbackend.model.User;
import com.biletal.biletalbackend.repository.PasswordResetTokenRepository;
import com.biletal.biletalbackend.repository.RegistrationTokenRepository;
import com.biletal.biletalbackend.security.JwtService;
import com.biletal.biletalbackend.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Controller
@RequestMapping("/")
@Tag(name = "Authentication", description = "Authentication and user registration APIs")
public class AuthController {
    private final AuthService authService;
    private final JwtService jwtService;
    private final RegistrationTokenRepository tokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    public AuthController(AuthService authService, JwtService jwtService, RegistrationTokenRepository tokenRepository, PasswordResetTokenRepository passwordResetTokenRepository) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.tokenRepository = tokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }

    @Operation(summary = "Activate user account", description = "Activates a user account using a registration token")
    @GetMapping("/activate")
    public String activateAccount(@RequestParam String token, Model model) {
        Optional<RegistrationToken> tokenOptional = tokenRepository.findByToken(token);
        
        if (tokenOptional.isEmpty()) {
            model.addAttribute("success", false);
            model.addAttribute("message", "Geçersiz aktivasyon bağlantısı.");
            return "activate";
        }
        
        RegistrationToken registrationToken = tokenOptional.get();
        
        if (registrationToken.isExpired()) {
            model.addAttribute("success", false);
            model.addAttribute("message", "Aktivasyon bağlantısının süresi dolmuştur. Lütfen yeni bir aktivasyon bağlantısı talep edin.");
            return "activate";
        }
        
        if (registrationToken.isUsed()) {
            model.addAttribute("success", false);
            model.addAttribute("message", "Bu aktivasyon bağlantısı daha önce kullanılmıştır.");
            return "activate";
        }
        
        // Show success page with form to create password
        model.addAttribute("success", true);
        model.addAttribute("token", token);
        model.addAttribute("email", registrationToken.getEmail());
        
        return "activate";
    }
    
    @Operation(summary = "Reset password page", description = "Shows password reset page for valid tokens")
    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam String token, Model model) {
        Optional<PasswordResetToken> tokenOptional = passwordResetTokenRepository.findByToken(token);
        
        if (tokenOptional.isEmpty()) {
            model.addAttribute("success", false);
            model.addAttribute("message", "Geçersiz şifre sıfırlama bağlantısı.");
            return "reset-password";
        }
        
        PasswordResetToken resetToken = tokenOptional.get();
        
        if (resetToken.isExpired()) {
            model.addAttribute("success", false);
            model.addAttribute("message", "Şifre sıfırlama bağlantısının süresi dolmuştur. Lütfen yeni bir bağlantı talep edin.");
            return "reset-password";
        }
        
        if (resetToken.isUsed()) {
            model.addAttribute("success", false);
            model.addAttribute("message", "Bu şifre sıfırlama bağlantısı daha önce kullanılmıştır.");
            return "reset-password";
        }
        
        // Show success page with form to reset password
        model.addAttribute("success", true);
        model.addAttribute("token", token);
        model.addAttribute("email", resetToken.getEmail());
        
        return "reset-password";
    }
    
    @Operation(summary = "Login page", description = "Shows login page")
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }
    
    @GetMapping("/")
    public String homePage() {
        return "redirect:/login";
    }

    @Operation(summary = "Update admin account", description = "Updates an admin account's information")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Account updated successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - requires admin role")
    })
    @PutMapping("/api/admin/update")
    public ResponseEntity<?> updateAdminAccount(@Valid @RequestBody AdminUpdateRequest request, HttpServletRequest httpRequest) {
        // Extract token from the Authorization header
        String header = httpRequest.getHeader(HttpHeaders.AUTHORIZATION);
        
        if (header == null || !header.startsWith("Bearer ")) {
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new com.biletal.biletalbackend.dto.ApiResponseDto("Yetkilendirme hatası: Token bulunamadı.", false));
        }
        
        String token = header.substring(7); // Remove "Bearer " prefix
        
        try {
            // Extract user ID and role from token
            Long userId = jwtService.extractClaim(token, claims -> claims.get("userId", Long.class));
            String role = jwtService.extractClaim(token, claims -> claims.get("role", String.class));
            
            if (userId == null) {
                return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new com.biletal.biletalbackend.dto.ApiResponseDto("Geçersiz token: Kullanıcı bilgisi bulunamadı.", false));
            }
            
            // Check if user is an admin
            if (!"SYSTEM_ADMIN".equals(role)) {
                return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(new com.biletal.biletalbackend.dto.ApiResponseDto("Bu işlem için admin yetkisi gereklidir.", false));
            }
            
            // Update the admin account
            User updatedAdmin = authService.updateAdminAccount(userId, request);
            
            return ResponseEntity
                .status(HttpStatus.OK)
                .body(new com.biletal.biletalbackend.dto.ApiResponseDto("Hesap bilgileriniz başarıyla güncellendi.", true));
                
        } catch (RuntimeException e) {
            if (e.getMessage().contains("E-posta adresi zaten kullanımda.")) {
                return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(new com.biletal.biletalbackend.dto.ApiResponseDto("E-posta adresi zaten kullanımda.", false));
            } else {
                return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new com.biletal.biletalbackend.dto.ApiResponseDto("Güncelleme sırasında bir hata oluştu: " + e.getMessage(), false));
            }
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new com.biletal.biletalbackend.dto.ApiResponseDto("Güncelleme sırasında bir hata oluştu.", false));
        }
    }

    @Operation(summary = "Admin login", description = "Authenticates an admin user and returns a JWT token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully authenticated", 
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
        @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/api/auth/admin/login")
    public ResponseEntity<?> adminLogin(@RequestBody LoginRequest loginRequest) {
        try {
            LoginResponse response = authService.authenticateAdmin(loginRequest);
            return ResponseEntity.ok(response);
        } catch (AuthenticationException e) {
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new LoginResponse(null, null, null, "Geçersiz kullanıcı bilgileri", null));
        } catch (RuntimeException e) {
            return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new LoginResponse(null, null, null, e.getMessage(), null));
        }
    }

    @Operation(summary = "User login", description = "Authenticates an end user and returns a JWT token")
    @PostMapping("/api/auth/login")
    public ResponseEntity<?> userLogin(@RequestBody LoginRequest loginRequest) {
        try {
            LoginResponse response = authService.authenticateUser(loginRequest);
            return ResponseEntity.ok(response);
        } catch (AuthenticationException e) {
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new LoginResponse(null, null, null, "Geçersiz kullanıcı bilgileri", null));   
        } catch (RuntimeException e) {
            return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new LoginResponse(null, null, null, e.getMessage(),  null));
        }
    }

    @Operation(summary = "User logout", description = "Logs out a user by invalidating their token")
    @PostMapping("/api/auth/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        
        if (header == null || !header.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body(new com.biletal.biletalbackend.dto.ApiResponseDto("Token sağlanmadı", false));
        }
        
        String token = header.substring(7);
        authService.logout(token);
        
        return ResponseEntity.ok(new com.biletal.biletalbackend.dto.ApiResponseDto("Çıkış başarılı", true));
    }

    @Operation(summary = "User registration", description = "Registers a new end user")
    @PostMapping("/api/auth/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegistrationRequest request) {
        try {
            Map<String, String> result = authService.registerUser(request);
            return ResponseEntity.ok(new com.biletal.biletalbackend.dto.ApiResponseDto(result.get("message"), true));
        } catch (RuntimeException e) {
            return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new com.biletal.biletalbackend.dto.ApiResponseDto(e.getMessage(), false));
        }
    }
    
    @Operation(summary = "Set password", description = "Sets a password for a newly registered user")
    @PostMapping("/api/set-password")
    public ResponseEntity<?> setPassword(@Valid @RequestBody PasswordRequest request) {
        try {
            Map<String, String> result = authService.createPassword(request);
            return ResponseEntity.ok(new com.biletal.biletalbackend.dto.ApiResponseDto(result.get("message"), true));
        } catch (RuntimeException e) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new com.biletal.biletalbackend.dto.ApiResponseDto(e.getMessage(), false));
        }
    }

    @Operation(summary = "Get Current User", description = "Get the current authenticated user's information")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User information retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/api/auth/current-user")
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        
        if (header == null || !header.startsWith("Bearer ")) {
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new com.biletal.biletalbackend.dto.ApiResponseDto("Yetkilendirme hatası: Token bulunamadı.", false));
        }
        
        String token = header.substring(7); // Remove "Bearer " prefix
        
        try {
            // Extract user ID and role from token
            Long userId = jwtService.extractClaim(token, claims -> claims.get("userId", Long.class));

            if (userId == null) {
                return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new com.biletal.biletalbackend.dto.ApiResponseDto("Geçersiz token: Kullanıcı bilgisi bulunamadı.", false));
            }
            
            // Get the current user information
            UserResponseDto currentUser = authService.getUserById(userId);
            
            return ResponseEntity.ok(currentUser);
                
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new com.biletal.biletalbackend.dto.ApiResponseDto("Kullanıcı bilgileri alınırken bir hata oluştu.", false));
        }
    }
    
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public com.biletal.biletalbackend.dto.ApiResponseDto handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        return new com.biletal.biletalbackend.dto.ApiResponseDto("Doğrulama hataları: " + errors, false);
    }
    
    @Operation(summary = "Forgot Password", description = "Send password reset link to user's email")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Password reset email sent successfully"),
        @ApiResponse(responseCode = "404", description = "Email not found"),
        @ApiResponse(responseCode = "409", description = "Valid reset token already exists")
    })
    @PostMapping("/api/auth/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            Map<String, String> result = authService.forgotPassword(request.getEmail());
            return ResponseEntity.ok(new ApiResponseDto(result.get("message"), true));
        } catch (RuntimeException e) {
            HttpStatus status = e.getMessage().contains("bulunamadı") ? HttpStatus.NOT_FOUND : HttpStatus.CONFLICT;
            return ResponseEntity
                .status(status)
                .body(new ApiResponseDto(e.getMessage(), false));
        }
    }
    
    @Operation(summary = "Reset Password", description = "Reset user password using reset token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Password reset successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid token or password validation failed"),
        @ApiResponse(responseCode = "410", description = "Token expired or already used")
    })
    @PostMapping("/api/auth/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            Map<String, String> result = authService.resetPassword(request);
            return ResponseEntity.ok(new ApiResponseDto(result.get("message"), true));
        } catch (RuntimeException e) {
            HttpStatus status = e.getMessage().contains("süresi dolmuş") || e.getMessage().contains("zaten kullanılmış") 
                ? HttpStatus.GONE : HttpStatus.BAD_REQUEST;
            return ResponseEntity
                .status(status)
                .body(new ApiResponseDto(e.getMessage(), false));
        }
    }
}
