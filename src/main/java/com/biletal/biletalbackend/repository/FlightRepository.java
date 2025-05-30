package com.biletal.biletalbackend.repository;

import com.biletal.biletalbackend.model.Flight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FlightRepository extends JpaRepository<Flight, Long>, JpaSpecificationExecutor<Flight> {
    
    // Find active flights only
    List<Flight> findByIsActiveTrueOrderByDepartureTimeAsc();
    
    // Find active flight by ID
    Optional<Flight> findByIdAndIsActiveTrue(Long id);
    
    // Find by flight number
    Optional<Flight> findByFlightNumber(String flightNumber);
    
    // Find active flight by flight number
    Optional<Flight> findByFlightNumberAndIsActiveTrue(String flightNumber);
    
    // Find flights by departure and arrival cities
    List<Flight> findByDepartureCityAndArrivalCityAndIsActiveTrueOrderByDepartureTimeAsc(
            String departureCity, String arrivalCity);
    
    // Find flights departing within a time range
    List<Flight> findByDepartureTimeBetweenAndIsActiveTrueOrderByDepartureTimeAsc(
            LocalDateTime startTime, LocalDateTime endTime);
    
    // Find flights by airline
    List<Flight> findByAirlineAndIsActiveTrueOrderByDepartureTimeAsc(String airline);
    
    // Find available flights (with available seats)
    List<Flight> findByAvailableSeatsGreaterThanAndIsActiveTrueOrderByDepartureTimeAsc(Integer seats);
}
