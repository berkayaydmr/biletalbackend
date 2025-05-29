package com.biletal.biletalbackend.dto;

import com.biletal.biletalbackend.model.Flight;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlightResponseDto {
    
    private Long id;
    private String flightNumber;
    private String airline;
    private String departureCity;
    private String arrivalCity;
    private String departureAirport;
    private String arrivalAirport;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private BigDecimal price;
    private Integer totalSeats;
    private Integer availableSeats;
    private Long durationInMinutes;
    private boolean isActive;
    private boolean isFull;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static FlightResponseDto fromFlight(Flight flight) {
        FlightResponseDto dto = new FlightResponseDto();
        dto.setId(flight.getId());
        dto.setFlightNumber(flight.getFlightNumber());
        dto.setAirline(flight.getAirline());
        dto.setDepartureCity(flight.getDepartureCity());
        dto.setArrivalCity(flight.getArrivalCity());
        dto.setDepartureAirport(flight.getDepartureAirport());
        dto.setArrivalAirport(flight.getArrivalAirport());
        dto.setDepartureTime(flight.getDepartureTime());
        dto.setArrivalTime(flight.getArrivalTime());
        dto.setPrice(flight.getPrice());
        dto.setTotalSeats(flight.getTotalSeats());
        dto.setAvailableSeats(flight.getAvailableSeats());
        dto.setDurationInMinutes(flight.getDurationInMinutes());
        dto.setActive(flight.isActive());
        dto.setFull(flight.isFull());
        dto.setCreatedAt(flight.getCreatedAt());
        dto.setUpdatedAt(flight.getUpdatedAt());
        return dto;
    }
}
