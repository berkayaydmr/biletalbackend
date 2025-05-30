package com.biletal.biletalbackend.service;

import com.biletal.biletalbackend.dto.FlightCreateRequest;
import com.biletal.biletalbackend.dto.FlightPageResponseDto;
import com.biletal.biletalbackend.dto.FlightResponseDto;
import com.biletal.biletalbackend.dto.FlightSearchRequest;
import com.biletal.biletalbackend.dto.FlightUpdateRequest;
import com.biletal.biletalbackend.model.Flight;
import com.biletal.biletalbackend.repository.FlightRepository;
import com.biletal.biletalbackend.specification.FlightSpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FlightService {
    
    private final FlightRepository flightRepository;
    
    @Autowired
    public FlightService(FlightRepository flightRepository) {
        this.flightRepository = flightRepository;
    }
    
    /**
     * Create a new flight
     * 
     * @param request The flight creation request
     * @return The created flight as DTO
     * @throws RuntimeException if flight number already exists or validation fails
     */
    @Transactional
    public FlightResponseDto createFlight(FlightCreateRequest request) {
        validateFlightRequest(request.getFlightNumber(), request.getDepartureTime(), 
                             request.getArrivalTime(), null);
        
        // Check if flight number already exists
        if (flightRepository.findByFlightNumber(request.getFlightNumber()).isPresent()) {
            throw new RuntimeException("Bu uçuş numarası zaten kullanımda: " + request.getFlightNumber());
        }
        
        Flight flight = new Flight();
        mapRequestToFlight(request, flight);
        flight.setAvailableSeats(request.getTotalSeats()); // Initially all seats are available
        flight.setActive(true);
        
        Flight savedFlight = flightRepository.save(flight);
        return FlightResponseDto.fromFlight(savedFlight);
    }
    
    /**
     * Update an existing flight
     * 
     * @param flightId The ID of the flight to update
     * @param request The flight update request
     * @return The updated flight as DTO
     * @throws RuntimeException if flight not found or validation fails
     */
    @Transactional
    public FlightResponseDto updateFlight(Long flightId, FlightUpdateRequest request) {
        Flight existingFlight = flightRepository.findById(flightId)
                .orElseThrow(() -> new RuntimeException("Uçuş bulunamadı: " + flightId));
        
        // Only validate if flight number is being updated
        if (request.getFlightNumber() != null) {
            // Check if flight number already exists (excluding current flight)
            flightRepository.findByFlightNumber(request.getFlightNumber())
                    .ifPresent(flight -> {
                        if (!flight.getId().equals(flightId)) {
                            throw new RuntimeException("Bu uçuş numarası zaten kullanımda: " + request.getFlightNumber());
                        }
                    });
        }
        
        // Validate times if they are being updated
        LocalDateTime departureTime = request.getDepartureTime() != null ? request.getDepartureTime() : existingFlight.getDepartureTime();
        LocalDateTime arrivalTime = request.getArrivalTime() != null ? request.getArrivalTime() : existingFlight.getArrivalTime();
        
        if (request.getDepartureTime() != null || request.getArrivalTime() != null) {
            if (departureTime.isAfter(arrivalTime)) {
                throw new RuntimeException("Kalkış zamanı varış zamanından önce olmalıdır");
            }
            
            if (departureTime.isBefore(LocalDateTime.now())) {
                throw new RuntimeException("Kalkış zamanı gelecekte olmalıdır");
            }
        }
        
        // Calculate the difference in total seats to update available seats
        int newAvailableSeats = existingFlight.getAvailableSeats();
        if (request.getTotalSeats() != null) {
            int seatDifference = request.getTotalSeats() - existingFlight.getTotalSeats();
            newAvailableSeats = existingFlight.getAvailableSeats() + seatDifference;
            
            // Ensure available seats don't go below 0
            if (newAvailableSeats < 0) {
                throw new RuntimeException("Mevcut rezervasyonlar nedeniyle koltuk sayısı azaltılamaz");
            }
        }
        
        // Map update request to existing flight
        mapUpdateRequestToFlight(request, existingFlight);
        existingFlight.setAvailableSeats(newAvailableSeats);
        
        if (request.getIsActive() != null) {
            existingFlight.setActive(request.getIsActive());
        }
        
        Flight updatedFlight = flightRepository.save(existingFlight);
        return FlightResponseDto.fromFlight(updatedFlight);
    }
    
    /**
     * Get flight by ID
     * 
     * @param flightId The ID of the flight
     * @return The flight as DTO
     * @throws RuntimeException if flight not found
     */
    public FlightResponseDto getFlightById(Long flightId) {
        Flight flight = flightRepository.findByIdAndIsActiveTrue(flightId)
                .orElseThrow(() -> new RuntimeException("Aktif uçuş bulunamadı: " + flightId));
        
        return FlightResponseDto.fromFlight(flight);
    }
    
    /**
     * Get all active flights with pagination
     * 
     * @param page Page number (0-based)
     * @param size Page size
     * @param sortBy Field to sort by
     * @param sortDirection Sort direction (ASC or DESC)
     * @return Paginated list of active flights as DTOs
     */
    public FlightPageResponseDto getAllActiveFlights(Integer page, Integer size, String sortBy, String sortDirection) {
        // Validate parameters
        if (page == null || page < 0) page = 0;
        if (size == null || size <= 0 || size > 100) size = 20;
        if (sortBy == null || sortBy.trim().isEmpty()) sortBy = "departureTime";
        if (sortDirection == null || 
            (!sortDirection.equalsIgnoreCase("ASC") && !sortDirection.equalsIgnoreCase("DESC"))) {
            sortDirection = "ASC";
        }
        
        // Create Sort object
        Sort sort = Sort.by(
            sortDirection.equalsIgnoreCase("DESC") ? Sort.Direction.DESC : Sort.Direction.ASC,
            sortBy
        );
        
        // Create Pageable object
        Pageable pageable = PageRequest.of(page, size, sort);
        
        // Get active flights with specification
        Specification<Flight> specification = FlightSpecification.getActiveFlights();
        Page<Flight> flightPage = flightRepository.findAll(specification, pageable);
        
        // Convert to DTOs
        List<FlightResponseDto> flightDtos = flightPage.getContent().stream()
                .map(FlightResponseDto::fromFlight)
                .collect(Collectors.toList());
        
        // Return paginated response
        return FlightPageResponseDto.of(
            flightDtos,
            flightPage.getNumber(),
            flightPage.getTotalPages(),
            flightPage.getTotalElements(),
            flightPage.getSize(),
            flightPage.hasNext(),
            flightPage.hasPrevious(),
            flightPage.isFirst(),
            flightPage.isLast()
        );
    }
    
    /**
     * Get all active flights (non-paginated version for backward compatibility)
     * 
     * @return List of active flights as DTOs
     */
    public List<FlightResponseDto> getAllActiveFlights() {
        List<Flight> flights = flightRepository.findByIsActiveTrueOrderByDepartureTimeAsc();
        return flights.stream()
                .map(FlightResponseDto::fromFlight)
                .collect(Collectors.toList());
    }
    
    /**
     * Search flights based on criteria with pagination
     * 
     * @param searchRequest The search criteria with pagination parameters
     * @return Paginated list of flights matching the criteria as DTOs
     */
    public FlightPageResponseDto searchFlights(FlightSearchRequest searchRequest) {
        // Validate and set default pagination parameters
        searchRequest.validatePagination();
        
        // Create Sort object based on request
        Sort sort = Sort.by(
            searchRequest.getSortDirection().equalsIgnoreCase("DESC") ? 
                Sort.Direction.DESC : Sort.Direction.ASC,
            searchRequest.getSortBy()
        );
        
        // Create Pageable object
        Pageable pageable = PageRequest.of(
            searchRequest.getPage(), 
            searchRequest.getSize(), 
            sort
        );
        
        // Create Specification based on search criteria
        Specification<Flight> specification;
        if (!searchRequest.hasSearchCriteria()) {
            // If no search criteria provided, return all active flights
            specification = FlightSpecification.getActiveFlights();
        } else {
            specification = FlightSpecification.getFlightsBySearchCriteria(searchRequest);
        }
        
        // Execute query with specification and pagination
        Page<Flight> flightPage = flightRepository.findAll(specification, pageable);
        
        // Convert to DTOs
        List<FlightResponseDto> flightDtos = flightPage.getContent().stream()
                .map(FlightResponseDto::fromFlight)
                .collect(Collectors.toList());
        
        // Return paginated response
        return FlightPageResponseDto.of(
            flightDtos,
            flightPage.getNumber(),
            flightPage.getTotalPages(),
            flightPage.getTotalElements(),
            flightPage.getSize(),
            flightPage.hasNext(),
            flightPage.hasPrevious(),
            flightPage.isFirst(),
            flightPage.isLast()
        );
    }
    
    /**
     * Search flights based on criteria (non-paginated version for backward compatibility)
     * 
     * @param searchRequest The search criteria
     * @return List of flights matching the criteria as DTOs
     */
    public List<FlightResponseDto> searchFlightsNonPaginated(FlightSearchRequest searchRequest) {
        // Create Specification based on search criteria
        Specification<Flight> specification;
        if (!searchRequest.hasSearchCriteria()) {
            // If no search criteria provided, return all active flights
            specification = FlightSpecification.getActiveFlights();
        } else {
            specification = FlightSpecification.getFlightsBySearchCriteria(searchRequest);
        }
        
        // Create Sort object
        Sort sort = Sort.by(Sort.Direction.ASC, "departureTime");
        
        // Execute query with specification
        List<Flight> flights = flightRepository.findAll(specification, sort);
        
        return flights.stream()
                .map(FlightResponseDto::fromFlight)
                .collect(Collectors.toList());
    }
    
    /**
     * Soft delete a flight (set isActive to false)
     * 
     * @param flightId The ID of the flight to delete
     * @throws RuntimeException if flight not found
     */
    @Transactional
    public void deleteFlight(Long flightId) {
        Flight flight = flightRepository.findById(flightId)
                .orElseThrow(() -> new RuntimeException("Uçuş bulunamadı: " + flightId));
        
        flight.setActive(false);
        flightRepository.save(flight);
    }
    
    /**
     * Check if a flight exists and is active
     * 
     * @param flightId The ID of the flight
     * @return true if flight exists and is active, false otherwise
     */
    public boolean isFlightActive(Long flightId) {
        return flightRepository.findByIdAndIsActiveTrue(flightId).isPresent();
    }
    
    /**
     * Reserve seats on a flight (decreases available seats)
     * This method would be called when someone books a ticket
     * 
     * @param flightId The ID of the flight
     * @param seatsToReserve Number of seats to reserve
     * @throws RuntimeException if flight not found or not enough seats available
     */
    @Transactional
    public void reserveSeats(Long flightId, int seatsToReserve) {
        Flight flight = flightRepository.findByIdAndIsActiveTrue(flightId)
                .orElseThrow(() -> new RuntimeException("Aktif uçuş bulunamadı: " + flightId));
        
        if (flight.getAvailableSeats() < seatsToReserve) {
            throw new RuntimeException("Yeterli uygun koltuk yok. Mevcut: " + flight.getAvailableSeats() + 
                                     ", İstenen: " + seatsToReserve);
        }
        
        flight.setAvailableSeats(flight.getAvailableSeats() - seatsToReserve);
        flightRepository.save(flight);
    }
    
    /**
     * Release seats on a flight (increases available seats)
     * This method would be called when someone cancels a ticket
     * 
     * @param flightId The ID of the flight
     * @param seatsToRelease Number of seats to release
     * @throws RuntimeException if flight not found
     */
    @Transactional
    public void releaseSeats(Long flightId, int seatsToRelease) {
        Flight flight = flightRepository.findByIdAndIsActiveTrue(flightId)
                .orElseThrow(() -> new RuntimeException("Aktif uçuş bulunamadı: " + flightId));
        
        int newAvailableSeats = flight.getAvailableSeats() + seatsToRelease;
        
        // Ensure available seats don't exceed total seats
        if (newAvailableSeats > flight.getTotalSeats()) {
            newAvailableSeats = flight.getTotalSeats();
        }
        
        flight.setAvailableSeats(newAvailableSeats);
        flightRepository.save(flight);
    }
    
    // Private helper methods
    
    private void validateFlightRequest(String flightNumber, LocalDateTime departureTime, 
                                     LocalDateTime arrivalTime, Long excludeFlightId) {
        if (flightNumber == null || flightNumber.trim().isEmpty()) {
            throw new RuntimeException("Uçuş numarası zorunludur");
        }
        
        if (departureTime == null) {
            throw new RuntimeException("Kalkış zamanı zorunludur");
        }
        
        if (arrivalTime == null) {
            throw new RuntimeException("Varış zamanı zorunludur");
        }
        
        if (departureTime.isAfter(arrivalTime)) {
            throw new RuntimeException("Kalkış zamanı varış zamanından önce olmalıdır");
        }
        
        if (departureTime.isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Kalkış zamanı gelecekte olmalıdır");
        }
    }
    
    private void mapRequestToFlight(FlightCreateRequest request, Flight flight) {
        flight.setFlightNumber(request.getFlightNumber());
        flight.setAirline(request.getAirline());
        flight.setDepartureCity(request.getDepartureCity());
        flight.setArrivalCity(request.getArrivalCity());
        flight.setDepartureAirport(request.getDepartureAirport());
        flight.setArrivalAirport(request.getArrivalAirport());
        flight.setDepartureTime(request.getDepartureTime());
        flight.setArrivalTime(request.getArrivalTime());
        flight.setPrice(request.getPrice());
        flight.setTotalSeats(request.getTotalSeats());
    }
    
    private void mapUpdateRequestToFlight(FlightUpdateRequest request, Flight flight) {
        if (request.getFlightNumber() != null) {
            flight.setFlightNumber(request.getFlightNumber());
        }
        if (request.getAirline() != null) {
            flight.setAirline(request.getAirline());
        }
        if (request.getDepartureCity() != null) {
            flight.setDepartureCity(request.getDepartureCity());
        }
        if (request.getArrivalCity() != null) {
            flight.setArrivalCity(request.getArrivalCity());
        }
        if (request.getDepartureAirport() != null) {
            flight.setDepartureAirport(request.getDepartureAirport());
        }
        if (request.getArrivalAirport() != null) {
            flight.setArrivalAirport(request.getArrivalAirport());
        }
        if (request.getDepartureTime() != null) {
            flight.setDepartureTime(request.getDepartureTime());
        }
        if (request.getArrivalTime() != null) {
            flight.setArrivalTime(request.getArrivalTime());
        }
        if (request.getPrice() != null) {
            flight.setPrice(request.getPrice());
        }
        if (request.getTotalSeats() != null) {
            flight.setTotalSeats(request.getTotalSeats());
        }
    }
    
    // Additional convenience methods using Specifications
    
    /**
     * Get flights by departure and arrival cities with pagination
     * 
     * @param departureCity Departure city
     * @param arrivalCity Arrival city
     * @param page Page number
     * @param size Page size
     * @return Paginated list of flights
     */
    public FlightPageResponseDto getFlightsByRoute(String departureCity, String arrivalCity, 
                                                  Integer page, Integer size) {
        if (page == null || page < 0) page = 0;
        if (size == null || size <= 0 || size > 100) size = 20;
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("departureTime").ascending());
        Specification<Flight> specification = FlightSpecification.getFlightsByDepartureAndArrivalCities(
            departureCity, arrivalCity);
        
        Page<Flight> flightPage = flightRepository.findAll(specification, pageable);
        
        List<FlightResponseDto> flightDtos = flightPage.getContent().stream()
                .map(FlightResponseDto::fromFlight)
                .collect(Collectors.toList());
        
        return FlightPageResponseDto.of(
            flightDtos,
            flightPage.getNumber(),
            flightPage.getTotalPages(),
            flightPage.getTotalElements(),
            flightPage.getSize(),
            flightPage.hasNext(),
            flightPage.hasPrevious(),
            flightPage.isFirst(),
            flightPage.isLast()
        );
    }
    
    /**
     * Get flights by airline with pagination
     * 
     * @param airline Airline name
     * @param page Page number
     * @param size Page size
     * @return Paginated list of flights
     */
    public FlightPageResponseDto getFlightsByAirline(String airline, Integer page, Integer size) {
        if (page == null || page < 0) page = 0;
        if (size == null || size <= 0 || size > 100) size = 20;
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("departureTime").ascending());
        Specification<Flight> specification = FlightSpecification.getFlightsByAirline(airline);
        
        Page<Flight> flightPage = flightRepository.findAll(specification, pageable);
        
        List<FlightResponseDto> flightDtos = flightPage.getContent().stream()
                .map(FlightResponseDto::fromFlight)
                .collect(Collectors.toList());
        
        return FlightPageResponseDto.of(
            flightDtos,
            flightPage.getNumber(),
            flightPage.getTotalPages(),
            flightPage.getTotalElements(),
            flightPage.getSize(),
            flightPage.hasNext(),
            flightPage.hasPrevious(),
            flightPage.isFirst(),
            flightPage.isLast()
        );
    }
    
    /**
     * Get flights within departure time range with pagination
     * 
     * @param startTime Start time
     * @param endTime End time
     * @param page Page number
     * @param size Page size
     * @return Paginated list of flights
     */
    public FlightPageResponseDto getFlightsByDepartureTimeRange(LocalDateTime startTime, LocalDateTime endTime,
                                                               Integer page, Integer size) {
        if (page == null || page < 0) page = 0;
        if (size == null || size <= 0 || size > 100) size = 20;
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("departureTime").ascending());
        Specification<Flight> specification = FlightSpecification.getFlightsByDepartureTimeRange(startTime, endTime);
        
        Page<Flight> flightPage = flightRepository.findAll(specification, pageable);
        
        List<FlightResponseDto> flightDtos = flightPage.getContent().stream()
                .map(FlightResponseDto::fromFlight)
                .collect(Collectors.toList());
        
        return FlightPageResponseDto.of(
            flightDtos,
            flightPage.getNumber(),
            flightPage.getTotalPages(),
            flightPage.getTotalElements(),
            flightPage.getSize(),
            flightPage.hasNext(),
            flightPage.hasPrevious(),
            flightPage.isFirst(),
            flightPage.isLast()
        );
    }
    
    /**
     * Get available flights with pagination
     * 
     * @param minAvailableSeats Minimum available seats
     * @param page Page number
     * @param size Page size
     * @return Paginated list of available flights
     */
    public FlightPageResponseDto getAvailableFlights(Integer minAvailableSeats, Integer page, Integer size) {
        if (minAvailableSeats == null) minAvailableSeats = 0;
        if (page == null || page < 0) page = 0;
        if (size == null || size <= 0 || size > 100) size = 20;
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("departureTime").ascending());
        Specification<Flight> specification = FlightSpecification.getAvailableFlights(minAvailableSeats);
        
        Page<Flight> flightPage = flightRepository.findAll(specification, pageable);
        
        List<FlightResponseDto> flightDtos = flightPage.getContent().stream()
                .map(FlightResponseDto::fromFlight)
                .collect(Collectors.toList());
        
        return FlightPageResponseDto.of(
            flightDtos,
            flightPage.getNumber(),
            flightPage.getTotalPages(),
            flightPage.getTotalElements(),
            flightPage.getSize(),
            flightPage.hasNext(),
            flightPage.hasPrevious(),
            flightPage.isFirst(),
            flightPage.isLast()
        );
    }
}
