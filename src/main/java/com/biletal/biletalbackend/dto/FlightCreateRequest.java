package com.biletal.biletalbackend.dto;

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
public class FlightCreateRequest {
    
    @NotBlank(message = "Uçuş numarası zorunludur")
    private String flightNumber;
    
    @NotBlank(message = "Havayolu şirketi zorunludur")
    private String airline;
    
    @NotBlank(message = "Kalkış şehri zorunludur")
    private String departureCity;
    
    @NotBlank(message = "Varış şehri zorunludur")
    private String arrivalCity;
    
    @NotBlank(message = "Kalkış havalimanı zorunludur")
    private String departureAirport;
    
    @NotBlank(message = "Varış havalimanı zorunludur")
    private String arrivalAirport;
    
    @NotNull(message = "Kalkış zamanı zorunludur")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime departureTime;
    
    @NotNull(message = "Varış zamanı zorunludur")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime arrivalTime;
    
    @NotNull(message = "Bilet fiyatı zorunludur")
    @Positive(message = "Bilet fiyatı pozitif olmalıdır")
    private BigDecimal price;
    
    @NotNull(message = "Toplam koltuk sayısı zorunludur")
    @Positive(message = "Toplam koltuk sayısı pozitif olmalıdır")
    private Integer totalSeats;
}
