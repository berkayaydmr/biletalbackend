package com.biletal.biletalbackend.dto;

import com.biletal.biletalbackend.custenum.BusType;
import com.fasterxml.jackson.annotation.JsonFormat;
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
public class BusExpeditionCreateRequest {
    
    @NotBlank(message = "Sefer numarası zorunludur")
    private String expeditionNumber;
    
    @NotBlank(message = "Otobüs şirketi zorunludur")
    private String busCompany;
    
    @NotBlank(message = "Kalkış şehri zorunludur")
    private String departureCity;
    
    @NotBlank(message = "Varış şehri zorunludur")
    private String arrivalCity;
    
    @NotBlank(message = "Kalkış terminali zorunludur")
    private String departureTerminal;
    
    @NotBlank(message = "Varış terminali zorunludur")
    private String arrivalTerminal;
    
    @NotNull(message = "Kalkış zamanı zorunludur")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime departureTime;
    
    @NotNull(message = "Varış zamanı zorunludur")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime arrivalTime;
    
    @NotNull(message = "Otobüs tipi zorunludur")
    private BusType busType;
    
    @NotNull(message = "Bilet fiyatı zorunludur")
    @Positive(message = "Bilet fiyatı pozitif olmalıdır")
    private BigDecimal price;
    
    @NotNull(message = "Toplam koltuk sayısı zorunludur")
    @Positive(message = "Toplam koltuk sayısı pozitif olmalıdır")
    private Integer totalSeats;
}
