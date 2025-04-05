package com.biletal.biletalbackend.service;

import com.biletal.biletalbackend.custenum.Gender;
import com.biletal.biletalbackend.custenum.Role;
import com.biletal.biletalbackend.dto.UserUpdateRequest;
import com.biletal.biletalbackend.model.User;
import com.biletal.biletalbackend.repository.UserRepository;
import com.biletal.biletalbackend.security.TokenWhitelistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.util.NoSuchElementException;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final TokenWhitelistService tokenWhitelistService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, TokenWhitelistService tokenWhitelistService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tokenWhitelistService = tokenWhitelistService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Soft delete a user account by setting isDeleted flag to true and removing all tokens
     *
     * @param userId The ID of the user to delete
     * @return The deleted user
     * @throws NoSuchElementException if the user is not found
     */
    @Transactional
    public User deleteUserAccount(Long userId) {
        User user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new NoSuchElementException("Kullanıcı bulunamadı."));

        // Soft delete the user
        user.setDeleted(true);
        User deletedUser = userRepository.save(user);

        // Remove all tokens for this user
        tokenWhitelistService.removeAllUserTokens(userId);

        return deletedUser;
    }
    
    /**
     * Update end user profile information
     * 
     * @param userId The ID of the user
     * @param request The update request containing the new profile information
     * @return The updated user
     * @throws NoSuchElementException if the user is not found
     * @throws IllegalArgumentException if the user is not an end user or validation fails
     */
    @Transactional
    public User updateUserProfile(Long userId, UserUpdateRequest request) {
        // Find the user by ID
        User user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new NoSuchElementException("Kullanıcı bulunamadı."));
        
        // Check if email is being changed and if it's already in use by another user
        if (request.getEmail() != null && !request.getEmail().isEmpty() && !user.getEmail().equals(request.getEmail())) {
            userRepository.findByEmailAndIsDeletedFalse(request.getEmail())
                .ifPresent(existingUser -> {
                    if (!existingUser.getId().equals(userId)) {
                        throw new IllegalArgumentException("E-posta adresi zaten kullanımda.");
                    }
                });
            user.setEmail(request.getEmail());
        }
        
        // Update user information if provided
        if (request.getFirstName() != null && !request.getFirstName().isEmpty()) {
            user.setFirstName(request.getFirstName());
        }
        
        if (request.getLastName() != null && !request.getLastName().isEmpty()) {
            user.setLastName(request.getLastName());
        }
        
        if (request.getPhone() != null && !request.getPhone().isEmpty()) {
            user.setPhone(request.getPhone());
        }
        
        // Update password if provided
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getBirthCountry() != null && !request.getBirthCountry().isEmpty()) {
            user.setBirthCountry(request.getBirthCountry());
        }

        if (request.getBirthCity() != null && !request.getBirthCity().isEmpty()) {
            user.setBirthCity(request.getBirthCity());
        }

        if (request.getGender() != null && !request.getGender().isEmpty()) {
            try {
                user.setGender(Gender.valueOf(request.getGender().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Geçersiz cinsiyet değeri.");
            }
        }

        if (request.getBirthDate() != null && !request.getBirthDate().isEmpty()) {
            LocalDate birthDate = LocalDate.parse(request.getBirthDate());
            if (birthDate.isAfter(LocalDate.now())) {
                throw new IllegalArgumentException("Doğum tarihi gelecekte olamaz.");
            }
            user.setBirthDate(birthDate);
        }

        if (request.getAdress() != null && !request.getAdress().isEmpty()) {    
            user.setAddress(request.getAdress());
        }

        // Save the updated user
        return userRepository.save(user);
    }
}