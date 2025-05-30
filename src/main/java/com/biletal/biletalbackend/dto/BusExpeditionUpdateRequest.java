package com.biletal.biletalbackend.dto;

import com.biletal.biletalbackend.custenum.BusType;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusExpeditionUpdateRequest {
    
    private String expeditionNumber;
    
    private String busCompany;
    
    private String departureCity;
    
    private String arrivalCity;
    
    private String departureTerminal;
    
    private String arrivalTerminal;
    
    private LocalDateTime departureTime;
    
    private LocalDateTime arrivalTime;
    
    private BusType busType;
    
    @Positive(message = "Bilet fiyatı pozitif olmalıdır")
    private BigDecimal price;
    
    @Positive(message = "Toplam koltuk sayısı pozitif olmalıdır")
    private Integer totalSeats;
    
    private Boolean isActive;
}
