package com.biletal.biletalbackend.dto;

import com.biletal.biletalbackend.custenum.BusType;
import com.biletal.biletalbackend.model.BusExpedition;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusExpeditionResponseDto {
    
    private Long id;
    private String expeditionNumber;
    private String busCompany;
    private String departureCity;
    private String arrivalCity;
    private String departureTerminal;
    private String arrivalTerminal;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private BusType busType;
    private BigDecimal price;
    private Integer totalSeats;
    private Integer availableSeats;
    private Long durationInMinutes;
    private boolean isActive;
    private boolean isFull;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static BusExpeditionResponseDto fromBusExpedition(BusExpedition expedition) {
        BusExpeditionResponseDto dto = new BusExpeditionResponseDto();
        dto.setId(expedition.getId());
        dto.setExpeditionNumber(expedition.getExpeditionNumber());
        dto.setBusCompany(expedition.getBusCompany());
        dto.setDepartureCity(expedition.getDepartureCity());
        dto.setArrivalCity(expedition.getArrivalCity());
        dto.setDepartureTerminal(expedition.getDepartureTerminal());
        dto.setArrivalTerminal(expedition.getArrivalTerminal());
        dto.setDepartureTime(expedition.getDepartureTime());
        dto.setArrivalTime(expedition.getArrivalTime());
        dto.setBusType(expedition.getBusType());
        dto.setPrice(expedition.getPrice());
        dto.setTotalSeats(expedition.getTotalSeats());
        dto.setAvailableSeats(expedition.getAvailableSeats());
        dto.setDurationInMinutes(expedition.getDurationInMinutes());
        dto.setActive(expedition.isActive());
        dto.setFull(expedition.isFull());
        dto.setCreatedAt(expedition.getCreatedAt());
        dto.setUpdatedAt(expedition.getUpdatedAt());
        return dto;
    }
}
