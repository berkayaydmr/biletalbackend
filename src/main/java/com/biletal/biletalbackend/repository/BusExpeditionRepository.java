package com.biletal.biletalbackend.repository;

import com.biletal.biletalbackend.model.BusExpedition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BusExpeditionRepository extends JpaRepository<BusExpedition, Long>, JpaSpecificationExecutor<BusExpedition> {
    
    // Find active expeditions only
    List<BusExpedition> findByIsActiveTrueOrderByDepartureTimeAsc();
    
    // Find active expedition by ID
    Optional<BusExpedition> findByIdAndIsActiveTrue(Long id);
    
    // Find by expedition number
    Optional<BusExpedition> findByExpeditionNumber(String expeditionNumber);
    
    // Find active expedition by expedition number
    Optional<BusExpedition> findByExpeditionNumberAndIsActiveTrue(String expeditionNumber);
    
    // Find expeditions by departure and arrival cities
    List<BusExpedition> findByDepartureCityAndArrivalCityAndIsActiveTrueOrderByDepartureTimeAsc(
            String departureCity, String arrivalCity);
    
    // Find expeditions departing within a time range
    List<BusExpedition> findByDepartureTimeBetweenAndIsActiveTrueOrderByDepartureTimeAsc(
            LocalDateTime startTime, LocalDateTime endTime);
    
    // Find expeditions by bus company
    List<BusExpedition> findByBusCompanyAndIsActiveTrueOrderByDepartureTimeAsc(String busCompany);
    
    // Find available expeditions (with available seats)
    List<BusExpedition> findByAvailableSeatsGreaterThanAndIsActiveTrueOrderByDepartureTimeAsc(Integer seats);
}
