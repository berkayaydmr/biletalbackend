package com.biletal.biletalbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlightUpdateRequest {
    
    private String flightNumber;
    
    private String airline;
    
    private String departureCity;
    
    private String arrivalCity;
    
    private String departureAirport;
    
    private String arrivalAirport;
    
    private LocalDateTime departureTime;
    
    private LocalDateTime arrivalTime;
    
    @Positive(message = "Bilet fiyatı pozitif olmalıdır")
    private BigDecimal price;
    
    @Positive(message = "Toplam koltuk sayısı pozitif olmalıdır")
    private Integer totalSeats;
    
    private Boolean isActive;
}
