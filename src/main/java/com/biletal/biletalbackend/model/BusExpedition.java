package com.biletal.biletalbackend.model;

import com.biletal.biletalbackend.custenum.BusType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bus_expeditions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusExpedition {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String expeditionNumber;
    
    @Column(nullable = false)
    private String busCompany;
    
    @Column(nullable = false)
    private String departureCity;
    
    @Column(nullable = false)
    private String arrivalCity;
    
    @Column(nullable = false)
    private String departureTerminal;
    
    @Column(nullable = false)
    private String arrivalTerminal;
    
    @Column(nullable = false)
    private LocalDateTime departureTime;
    
    @Column(nullable = false)
    private LocalDateTime arrivalTime;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BusType busType;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
    
    @Column(nullable = false)
    private Integer totalSeats;
    
    @Column(nullable = false)
    private Integer availableSeats;
    
    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean isActive = true;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Helper method to calculate duration
    public Long getDurationInMinutes() {
        return java.time.Duration.between(departureTime, arrivalTime).toMinutes();
    }
    
    // Helper method to check if expedition is full
    public boolean isFull() {
        return availableSeats <= 0;
    }
}
