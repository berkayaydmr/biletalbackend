package com.biletal.biletalbackend.service;

import com.biletal.biletalbackend.custenum.Gender;
import com.biletal.biletalbackend.custenum.Role;
import com.biletal.biletalbackend.dto.AdminUpdateRequest;
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
import com.biletal.biletalbackend.repository.UserRepository;
import com.biletal.biletalbackend.security.JwtService;
import com.biletal.biletalbackend.security.TokenWhitelistService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final TokenWhitelistService tokenWhitelistService;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final RegistrationTokenRepository tokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    
    @Value("${application.baseUrl}")
    private String baseUrl;

    public AuthService(AuthenticationManager authenticationManager, 
                      UserRepository userRepository, 
                      JwtService jwtService,
                      TokenWhitelistService tokenWhitelistService,
                      PasswordEncoder passwordEncoder,
                      JavaMailSender mailSender,
                      RegistrationTokenRepository tokenRepository,
                      PasswordResetTokenRepository passwordResetTokenRepository) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.tokenWhitelistService = tokenWhitelistService;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
        this.tokenRepository = tokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }
    
    public LoginResponse authenticateAdmin(LoginRequest request) {
        // Authenticate with Spring Security
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        
        // Get the user and verify they're a system admin
        User user = userRepository.findByEmailAndIsDeletedFalse(request.getEmail())
            .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));
        
        if (Role.SYSTEM_ADMIN != user.getRole()) {
            throw new RuntimeException("Kullanıcı sistem yöneticisi olarak yetkilendirilmemiş");
        }
        
        // Generate JWT token
        String token = jwtService.generateToken(user.getEmail(), user.getId(), user.getRole().toString());
        
        // Add token to whitelist
        tokenWhitelistService.addToken(token, user.getId());
        
        return new LoginResponse(token, user.getEmail(), user.getRole().toString(), "Giriş başarılı", UserResponseDto.fromUser(user));
    }
    
    public LoginResponse authenticateUser(LoginRequest request) {
        // Authenticate with Spring Security
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        
        // Get the user
        User user = userRepository.findByEmailAndIsDeletedFalse(request.getEmail())
            .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));
        
        // Generate JWT token
        String token = jwtService.generateToken(user.getEmail(), user.getId(), user.getRole().toString());
        
        // Add token to whitelist
        tokenWhitelistService.addToken(token, user.getId());
        
        return new LoginResponse(token, user.getEmail(), user.getRole().toString(), "Giriş başarılı", UserResponseDto.fromUser(user));
    }
    
    /**
     * Authenticate end user exclusively for /api/login endpoint
     */
    public LoginResponse authenticateEndUser(LoginRequest request) {
        // Authenticate with Spring Security
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        
        // Get the user and verify they're an end user
        User user = userRepository.findByEmailAndIsDeletedFalse(request.getEmail())
            .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));
        
        if (Role.END_USER != user.getRole()) {
            throw new RuntimeException("Bu endpoint yalnızca son kullanıcılar içindir");
        }
        
        // Generate JWT token
        String token = jwtService.generateToken(user.getEmail(), user.getId(), user.getRole().toString());
        
        // Add token to whitelist
        tokenWhitelistService.addToken(token, user.getId());
        
        return new LoginResponse(token, user.getEmail(), user.getRole().toString(), "Giriş başarılı",UserResponseDto.fromUser(user));
    }
    
    public void logout(String token) {
        tokenWhitelistService.removeToken(token);
    }

    public UserResponseDto getUserById(Long userId) {
        User user = userRepository.findByIdAndIsDeletedFalse(userId)
            .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));
        
        return UserResponseDto.fromUser(user);
    }
    
    @Transactional
    public Map<String, String> registerUser(RegistrationRequest request) {
        // Validate required fields
        validateRegistration(request);
        
        // Check if email is already in use
        if (userRepository.findByEmailAndIsDeletedFalse(request.getEmail()).isPresent()) {
            throw new RuntimeException("Bu e-posta adresi zaten kullanımda");
        }
        
        // Create user without password
        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setBirthDate(request.getBirthDate());
        user.setBirthCountry(request.getBirthCountry());
        user.setBirthCity(request.getBirthCity());
        user.setGender(Gender.valueOf(request.getGender().toUpperCase()));
        user.setEmail(request.getEmail());
        user.setAddress(request.getAddress());
        user.setPhone(request.getPhone());
        user.setRole(Role.END_USER);
        user.setPassword(""); // Temporary empty password
        user.setDeleted(false); // Explicitly set as not deleted
        
        User savedUser = userRepository.save(user);
        
        // Generate registration token
        String token = UUID.randomUUID().toString();
        RegistrationToken registrationToken = new RegistrationToken(
            token,
            user.getEmail(),
            savedUser,
            LocalDateTime.now().plusHours(24) // Token valid for 24 hours
        );
        
        tokenRepository.save(registrationToken);
        
        // Send confirmation email
        sendRegistrationEmail(user.getEmail(), token);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Kayıt işlemi başarılı! Lütfen e-posta adresinizi kontrol edin.");
        return response;
    }
    
    private void validateRegistration(RegistrationRequest request) {
        if (request.getFirstName() == null || request.getFirstName().trim().isEmpty()) {
            throw new RuntimeException("İsim alanı zorunludur");
        }
        if (request.getLastName() == null || request.getLastName().trim().isEmpty()) {
            throw new RuntimeException("Soyisim alanı zorunludur");
        }
        if (request.getBirthDate() == null) {
            throw new RuntimeException("Doğum tarihi zorunludur");
        }
        if (request.getBirthCountry() == null || request.getBirthCountry().trim().isEmpty()) {
            throw new RuntimeException("Doğum ülkesi zorunludur");
        }
        if (request.getBirthCity() == null || request.getBirthCity().trim().isEmpty()) {
            throw new RuntimeException("Doğum şehri zorunludur");
        }
        if (request.getGender() == null) {
            throw new RuntimeException("Cinsiyet zorunludur");
        }
        if (request.getEmail() == null || request.getEmail().trim().isEmpty() || !request.getEmail().contains("@")) {
            throw new RuntimeException("Geçerli bir e-posta adresi girin");
        }
    }
    
    private void sendRegistrationEmail(String email, String token) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Bilet Al - Hesap Aktivasyonu");
            message.setText("Merhaba,\n\n" +
                    "Bilet Al'a hoş geldiniz! Hesabınızı aktifleştirmek için lütfen aşağıdaki bağlantıya tıklayın:\n\n" +
                    baseUrl + "/activate?token=" + token + "\n\n" +
                    "Bu bağlantı 24 saat boyunca geçerlidir.\n\n" +
                    "Saygılarımızla,\n" +
                    "Bilet Al Ekibi");
            
            mailSender.send(message);
            System.out.println("✅ Registration email sent successfully to: " + email);
        } catch (Exception e) {
            // Log email details for testing purposes
            System.out.println("📧 Registration Email Details:");
            System.out.println("To: " + email);
            System.out.println("Activation Link: " + baseUrl + "/activate?token=" + token);
            System.out.println("❌ Email sending failed (using fallback): " + e.getMessage());
        }
    }
    
    @Transactional
    public Map<String, String> createPassword(PasswordRequest request) {
        if (request.getPassword() == null || request.getPassword().trim().length() < 6) {
            throw new RuntimeException("Şifre en az 6 karakter olmalıdır");
        }
        
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Şifreler eşleşmiyor");
        }
        
        // Find token
        RegistrationToken token = tokenRepository.findByToken(request.getToken())
            .orElseThrow(() -> new RuntimeException("Geçersiz veya süresi dolmuş token"));
        
        if (token.isUsed()) {
            throw new RuntimeException("Bu token zaten kullanılmış");
        }
        
        // Update user's password
        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);
        
        // Mark token as used
        token.setUsed(true);
        tokenRepository.save(token);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Şifre başarıyla oluşturuldu. Şimdi giriş yapabilirsiniz.");
        return response;
    }

    /**
     * Update admin account information
     * 
     * @param adminId The ID of the admin user
     * @param request The update request containing the new account information
     * @return The updated admin user
     * @throws RuntimeException if the email is already in use or the user is not an admin
     */
    @Transactional
    public User updateAdminAccount(Long adminId, AdminUpdateRequest request) {
        // Find the user by ID
        User admin = userRepository.findById(adminId)
            .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));
        
        // Verify the user is an admin
        if (Role.SYSTEM_ADMIN != admin.getRole()) {
            throw new RuntimeException("Bu işlem için admin yetkisi gereklidir");
        }
        
        // Check if email is being changed and if it's already in use by another user
        if (!admin.getEmail().equals(request.getEmail())) {
            userRepository.findByEmailAndIsDeletedFalse(request.getEmail())
                .ifPresent(user -> {
                    if (!user.getId().equals(adminId)) {
                        throw new RuntimeException("E-posta adresi zaten kullanımda.");
                    }
                });
        }
        
        // Update admin information
        admin.setFirstName(request.getFirstName());
        admin.setLastName(request.getLastName());
        admin.setEmail(request.getEmail());
        admin.setPhone(request.getPhone());
        
        // Update password if provided
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            admin.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        
        // Save the updated admin
        User updatedAdmin = userRepository.save(admin);
        
        return updatedAdmin;
    }
    
    /**
     * Send password reset email to user
     * 
     * @param email The email address to send reset link to
     * @return Success message
     * @throws RuntimeException if email is not found
     */
    @Transactional
    public Map<String, String> forgotPassword(String email) {
        // Find user by email
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
            .orElseThrow(() -> new RuntimeException("Bu e-posta adresi ile kayıtlı kullanıcı bulunamadı"));
        
        // Check if there's an existing valid token
        Optional<PasswordResetToken> existingToken = 
            passwordResetTokenRepository.findByEmailAndUsedFalseAndExpiryDateAfter(email, LocalDateTime.now());
        
        if (existingToken.isPresent()) {
            throw new RuntimeException("Zaten geçerli bir şifre sıfırlama bağlantınız bulunmaktadır. E-postanızı kontrol edin.");
        }
        
        // Generate new reset token
        String token = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusHours(1); // Token expires in 1 hour
        
        PasswordResetToken resetToken = new PasswordResetToken(token, email, user, expiryDate);
        passwordResetTokenRepository.save(resetToken);
        
        // Send password reset email
        sendPasswordResetEmail(email, token);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Şifre sıfırlama bağlantısı e-posta adresinize gönderildi.");
        return response;
    }
    
    /**
     * Reset user password using reset token
     * 
     * @param request Reset password request containing token and new password
     * @return Success message
     * @throws RuntimeException if token is invalid, expired, or passwords don't match
     */
    @Transactional
    public Map<String, String> resetPassword(ResetPasswordRequest request) {
        if (request.getPassword() == null || request.getPassword().trim().length() < 6) {
            throw new RuntimeException("Şifre en az 6 karakter olmalıdır");
        }
        
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Şifreler eşleşmiyor");
        }
        
        // Find and validate token
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
            .orElseThrow(() -> new RuntimeException("Geçersiz veya süresi dolmuş şifre sıfırlama bağlantısı"));
        
        if (resetToken.isUsed()) {
            throw new RuntimeException("Bu şifre sıfırlama bağlantısı zaten kullanılmış");
        }
        
        if (resetToken.isExpired()) {
            throw new RuntimeException("Şifre sıfırlama bağlantısının süresi dolmuş. Yeni bir bağlantı talep edin.");
        }
        
        // Update user's password
        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);
        
        // Mark token as used
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Şifreniz başarıyla sıfırlandı. Şimdi yeni şifrenizle giriş yapabilirsiniz.");
        return response;
    }
    
    /**
     * Send password reset email
     * 
     * @param email Email address to send to
     * @param token Reset token
     */
    private void sendPasswordResetEmail(String email, String token) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Bilet Al - Şifre Sıfırlama");
            message.setText("Merhaba,\n\n" +
                    "Şifre sıfırlama talebiniz alınmıştır. Şifrenizi sıfırlamak için lütfen aşağıdaki bağlantıya tıklayın:\n\n" +
                    baseUrl + "/reset-password?token=" + token + "\n\n" +
                    "Bu bağlantı 1 saat boyunca geçerlidir.\n\n" +
                    "Eğer bu talebi siz yapmadıysanız, bu e-postayı görmezden gelebilirsiniz.\n\n" +
                    "Saygılarımızla,\n" +
                    "Bilet Al Ekibi");
            
            mailSender.send(message);
            System.out.println("✅ Password reset email sent successfully to: " + email);
        } catch (Exception e) {
            // Log email details for testing purposes
            System.out.println("📧 Password Reset Email Details:");
            System.out.println("To: " + email);
            System.out.println("Reset Link: " + baseUrl + "/reset-password?token=" + token);
            System.out.println("❌ Email sending failed (using fallback): " + e.getMessage());
        }
    }
}
