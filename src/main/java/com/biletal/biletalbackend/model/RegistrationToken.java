package com.biletal.biletalbackend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "registration_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String token;
    
    @Column(nullable = false)
    private String email;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    @Column(nullable = false)
    private LocalDateTime expiryDate;
    
    @Column(nullable = false)
    private boolean used;
    
    public RegistrationToken(String token, String email, User user, LocalDateTime expiryDate) {
        this.token = token;
        this.email = email;
        this.user = user;
        this.expiryDate = expiryDate;
        this.used = false;
    }
    
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }
}