package com.biletal.biletalbackend.service;

import com.biletal.biletalbackend.dto.BusExpeditionCreateRequest;
import com.biletal.biletalbackend.dto.BusExpeditionPageResponseDto;
import com.biletal.biletalbackend.dto.BusExpeditionResponseDto;
import com.biletal.biletalbackend.dto.BusExpeditionSearchRequest;
import com.biletal.biletalbackend.dto.BusExpeditionUpdateRequest;
import com.biletal.biletalbackend.model.BusExpedition;
import com.biletal.biletalbackend.repository.BusExpeditionRepository;
import com.biletal.biletalbackend.specification.BusExpeditionSpecification;
import com.biletal.biletalbackend.custenum.BusType;
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
public class BusExpeditionService {
    
    private final BusExpeditionRepository busExpeditionRepository;
    
    @Autowired
    public BusExpeditionService(BusExpeditionRepository busExpeditionRepository) {
        this.busExpeditionRepository = busExpeditionRepository;
    }
    
    /**
     * Create a new bus expedition
     * 
     * @param request The bus expedition creation request
     * @return The created bus expedition as DTO
     * @throws RuntimeException if expedition number already exists or validation fails
     */
    @Transactional
    public BusExpeditionResponseDto createBusExpedition(BusExpeditionCreateRequest request) {
        validateBusExpeditionRequest(request.getExpeditionNumber(), request.getDepartureTime(), 
                                   request.getArrivalTime(), null);
        
        // Check if expedition number already exists
        if (busExpeditionRepository.findByExpeditionNumber(request.getExpeditionNumber()).isPresent()) {
            throw new RuntimeException("Bu sefer numarası zaten kullanımda: " + request.getExpeditionNumber());
        }
        
        BusExpedition busExpedition = new BusExpedition();
        mapRequestToBusExpedition(request, busExpedition);
        busExpedition.setAvailableSeats(request.getTotalSeats()); // Initially all seats are available
        busExpedition.setActive(true);
        
        BusExpedition savedBusExpedition = busExpeditionRepository.save(busExpedition);
        return BusExpeditionResponseDto.fromBusExpedition(savedBusExpedition);
    }
    
    /**
     * Update an existing bus expedition
     * 
     * @param expeditionId The ID of the bus expedition to update
     * @param request The bus expedition update request
     * @return The updated bus expedition as DTO
     * @throws RuntimeException if expedition not found or validation fails
     */
    @Transactional
    public BusExpeditionResponseDto updateBusExpedition(Long expeditionId, BusExpeditionUpdateRequest request) {
        BusExpedition existingExpedition = busExpeditionRepository.findById(expeditionId)
                .orElseThrow(() -> new RuntimeException("Otobüs seferi bulunamadı: " + expeditionId));
        
        // Only validate if expedition number is being updated
        if (request.getExpeditionNumber() != null) {
            // Check if expedition number already exists (excluding current expedition)
            busExpeditionRepository.findByExpeditionNumber(request.getExpeditionNumber())
                    .ifPresent(expedition -> {
                        if (!expedition.getId().equals(expeditionId)) {
                            throw new RuntimeException("Bu sefer numarası zaten kullanımda: " + request.getExpeditionNumber());
                        }
                    });
        }
        
        // Validate times if they are being updated
        LocalDateTime departureTime = request.getDepartureTime() != null ? request.getDepartureTime() : existingExpedition.getDepartureTime();
        LocalDateTime arrivalTime = request.getArrivalTime() != null ? request.getArrivalTime() : existingExpedition.getArrivalTime();
        
        if (request.getDepartureTime() != null || request.getArrivalTime() != null) {
            if (departureTime.isAfter(arrivalTime)) {
                throw new RuntimeException("Kalkış zamanı varış zamanından önce olmalıdır");
            }
            
            if (departureTime.isBefore(LocalDateTime.now())) {
                throw new RuntimeException("Kalkış zamanı gelecekte olmalıdır");
            }
        }
        
        // Calculate the difference in total seats to update available seats
        int newAvailableSeats = existingExpedition.getAvailableSeats();
        if (request.getTotalSeats() != null) {
            int seatDifference = request.getTotalSeats() - existingExpedition.getTotalSeats();
            newAvailableSeats = existingExpedition.getAvailableSeats() + seatDifference;
            
            // Ensure available seats don't go below 0
            if (newAvailableSeats < 0) {
                throw new RuntimeException("Mevcut rezervasyonlar nedeniyle koltuk sayısı azaltılamaz");
            }
        }
        
        // Map update request to existing expedition
        mapUpdateRequestToBusExpedition(request, existingExpedition);
        existingExpedition.setAvailableSeats(newAvailableSeats);
        
        if (request.getIsActive() != null) {
            existingExpedition.setActive(request.getIsActive());
        }
        
        BusExpedition updatedExpedition = busExpeditionRepository.save(existingExpedition);
        return BusExpeditionResponseDto.fromBusExpedition(updatedExpedition);
    }
    
    /**
     * Get bus expedition by ID
     * 
     * @param expeditionId The ID of the bus expedition
     * @return The bus expedition as DTO
     * @throws RuntimeException if expedition not found
     */
    public BusExpeditionResponseDto getBusExpeditionById(Long expeditionId) {
        BusExpedition expedition = busExpeditionRepository.findByIdAndIsActiveTrue(expeditionId)
                .orElseThrow(() -> new RuntimeException("Aktif otobüs seferi bulunamadı: " + expeditionId));
        
        return BusExpeditionResponseDto.fromBusExpedition(expedition);
    }
    
    /**
     * Get all active bus expeditions with pagination
     * 
     * @param page Page number (0-based)
     * @param size Page size
     * @param sortBy Field to sort by
     * @param sortDirection Sort direction (ASC or DESC)
     * @return Paginated list of active bus expeditions as DTOs
     */
    public BusExpeditionPageResponseDto getAllActiveBusExpeditions(Integer page, Integer size, String sortBy, String sortDirection) {
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
        
        // Get active expeditions with specification
        Specification<BusExpedition> specification = BusExpeditionSpecification.getActiveBusExpeditions();
        Page<BusExpedition> expeditionPage = busExpeditionRepository.findAll(specification, pageable);
        
        // Convert to DTOs
        List<BusExpeditionResponseDto> expeditionDtos = expeditionPage.getContent().stream()
                .map(BusExpeditionResponseDto::fromBusExpedition)
                .collect(Collectors.toList());
        
        // Return paginated response
        return BusExpeditionPageResponseDto.of(
            expeditionDtos,
            expeditionPage.getNumber(),
            expeditionPage.getTotalPages(),
            expeditionPage.getTotalElements(),
            expeditionPage.getSize(),
            expeditionPage.hasNext(),
            expeditionPage.hasPrevious(),
            expeditionPage.isFirst(),
            expeditionPage.isLast()
        );
    }
    
    /**
     * Get all active bus expeditions (non-paginated version for backward compatibility)
     * 
     * @return List of active bus expeditions as DTOs
     */
    public List<BusExpeditionResponseDto> getAllActiveBusExpeditions() {
        List<BusExpedition> expeditions = busExpeditionRepository.findByIsActiveTrueOrderByDepartureTimeAsc();
        return expeditions.stream()
                .map(BusExpeditionResponseDto::fromBusExpedition)
                .collect(Collectors.toList());
    }
    
    /**
     * Search bus expeditions based on criteria with pagination
     * 
     * @param searchRequest The search criteria with pagination parameters
     * @return Paginated list of expeditions matching the criteria as DTOs
     */
    public BusExpeditionPageResponseDto searchBusExpeditions(BusExpeditionSearchRequest searchRequest) {
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
        Specification<BusExpedition> specification;
        if (!searchRequest.hasSearchCriteria()) {
            // If no search criteria provided, return all active expeditions
            specification = BusExpeditionSpecification.getActiveBusExpeditions();
        } else {
            specification = BusExpeditionSpecification.getBusExpeditionsBySearchCriteria(searchRequest);
        }
        
        // Execute query with specification and pagination
        Page<BusExpedition> expeditionPage = busExpeditionRepository.findAll(specification, pageable);
        
        // Convert to DTOs
        List<BusExpeditionResponseDto> expeditionDtos = expeditionPage.getContent().stream()
                .map(BusExpeditionResponseDto::fromBusExpedition)
                .collect(Collectors.toList());
        
        // Return paginated response
        return BusExpeditionPageResponseDto.of(
            expeditionDtos,
            expeditionPage.getNumber(),
            expeditionPage.getTotalPages(),
            expeditionPage.getTotalElements(),
            expeditionPage.getSize(),
            expeditionPage.hasNext(),
            expeditionPage.hasPrevious(),
            expeditionPage.isFirst(),
            expeditionPage.isLast()
        );
    }
    
    /**
     * Search bus expeditions based on criteria (non-paginated version for backward compatibility)
     * 
     * @param searchRequest The search criteria
     * @return List of expeditions matching the criteria as DTOs
     */
    public List<BusExpeditionResponseDto> searchBusExpeditionsNonPaginated(BusExpeditionSearchRequest searchRequest) {
        // Create Specification based on search criteria
        Specification<BusExpedition> specification;
        if (!searchRequest.hasSearchCriteria()) {
            // If no search criteria provided, return all active expeditions
            specification = BusExpeditionSpecification.getActiveBusExpeditions();
        } else {
            specification = BusExpeditionSpecification.getBusExpeditionsBySearchCriteria(searchRequest);
        }
        
        // Create Sort object
        Sort sort = Sort.by(Sort.Direction.ASC, "departureTime");
        
        // Execute query with specification
        List<BusExpedition> expeditions = busExpeditionRepository.findAll(specification, sort);
        
        return expeditions.stream()
                .map(BusExpeditionResponseDto::fromBusExpedition)
                .collect(Collectors.toList());
    }
    
    /**
     * Soft delete a bus expedition (set isActive to false)
     * 
     * @param expeditionId The ID of the expedition to delete
     * @throws RuntimeException if expedition not found
     */
    @Transactional
    public void deleteBusExpedition(Long expeditionId) {
        BusExpedition expedition = busExpeditionRepository.findById(expeditionId)
                .orElseThrow(() -> new RuntimeException("Otobüs seferi bulunamadı: " + expeditionId));
        
        expedition.setActive(false);
        busExpeditionRepository.save(expedition);
    }
    
    /**
     * Check if a bus expedition exists and is active
     * 
     * @param expeditionId The ID of the expedition
     * @return true if expedition exists and is active, false otherwise
     */
    public boolean isBusExpeditionActive(Long expeditionId) {
        return busExpeditionRepository.findByIdAndIsActiveTrue(expeditionId).isPresent();
    }
    
    /**
     * Reserve seats on a bus expedition (decreases available seats)
     * This method would be called when someone books a ticket
     * 
     * @param expeditionId The ID of the expedition
     * @param seatsToReserve Number of seats to reserve
     * @return Updated bus expedition details
     * @throws RuntimeException if expedition not found or not enough seats available
     */
    @Transactional
    public BusExpeditionResponseDto reserveSeats(Long expeditionId, int seatsToReserve) {
        BusExpedition expedition = busExpeditionRepository.findByIdAndIsActiveTrue(expeditionId)
                .orElseThrow(() -> new RuntimeException("Aktif otobüs seferi bulunamadı: " + expeditionId));
        
        if (expedition.getAvailableSeats() < seatsToReserve) {
            throw new RuntimeException("Yeterli uygun koltuk yok. Mevcut: " + expedition.getAvailableSeats() + 
                                     ", İstenen: " + seatsToReserve);
        }
        
        expedition.setAvailableSeats(expedition.getAvailableSeats() - seatsToReserve);
        BusExpedition savedExpedition = busExpeditionRepository.save(expedition);
        return BusExpeditionResponseDto.fromBusExpedition(savedExpedition);
    }
    
    /**
     * Release seats on a bus expedition (increases available seats)
     * This method would be called when someone cancels a ticket
     * 
     * @param expeditionId The ID of the expedition
     * @param seatsToRelease Number of seats to release
     * @return Updated bus expedition details
     * @throws RuntimeException if expedition not found
     */
    @Transactional
    public BusExpeditionResponseDto releaseSeats(Long expeditionId, int seatsToRelease) {
        BusExpedition expedition = busExpeditionRepository.findByIdAndIsActiveTrue(expeditionId)
                .orElseThrow(() -> new RuntimeException("Aktif otobüs seferi bulunamadı: " + expeditionId));
        
        int newAvailableSeats = expedition.getAvailableSeats() + seatsToRelease;
        
        // Ensure available seats don't exceed total seats
        if (newAvailableSeats > expedition.getTotalSeats()) {
            newAvailableSeats = expedition.getTotalSeats();
        }
        
        expedition.setAvailableSeats(newAvailableSeats);
        BusExpedition savedExpedition = busExpeditionRepository.save(expedition);
        return BusExpeditionResponseDto.fromBusExpedition(savedExpedition);
    }
    
    // Private helper methods
    
    private void validateBusExpeditionRequest(String expeditionNumber, LocalDateTime departureTime, 
                                            LocalDateTime arrivalTime, Long excludeExpeditionId) {
        if (expeditionNumber == null || expeditionNumber.trim().isEmpty()) {
            throw new RuntimeException("Sefer numarası zorunludur");
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
    
    private void mapRequestToBusExpedition(BusExpeditionCreateRequest request, BusExpedition expedition) {
        expedition.setExpeditionNumber(request.getExpeditionNumber());
        expedition.setBusCompany(request.getBusCompany());
        expedition.setDepartureCity(request.getDepartureCity());
        expedition.setArrivalCity(request.getArrivalCity());
        expedition.setDepartureTerminal(request.getDepartureTerminal());
        expedition.setArrivalTerminal(request.getArrivalTerminal());
        expedition.setDepartureTime(request.getDepartureTime());
        expedition.setArrivalTime(request.getArrivalTime());
        expedition.setBusType(request.getBusType());
        expedition.setPrice(request.getPrice());
        expedition.setTotalSeats(request.getTotalSeats());
    }
    
    /**
     * Map update request to existing bus expedition
     * 
     * @param request The update request
     * @param expedition The existing expedition to update
     */
    private void mapUpdateRequestToBusExpedition(BusExpeditionUpdateRequest request, BusExpedition expedition) {
        if (request.getExpeditionNumber() != null) {
            expedition.setExpeditionNumber(request.getExpeditionNumber());
        }
        if (request.getBusCompany() != null) {
            expedition.setBusCompany(request.getBusCompany());
        }
        if (request.getDepartureCity() != null) {
            expedition.setDepartureCity(request.getDepartureCity());
        }
        if (request.getArrivalCity() != null) {
            expedition.setArrivalCity(request.getArrivalCity());
        }
        if (request.getDepartureTerminal() != null) {
            expedition.setDepartureTerminal(request.getDepartureTerminal());
        }
        if (request.getArrivalTerminal() != null) {
            expedition.setArrivalTerminal(request.getArrivalTerminal());
        }
        if (request.getDepartureTime() != null) {
            expedition.setDepartureTime(request.getDepartureTime());
        }
        if (request.getArrivalTime() != null) {
            expedition.setArrivalTime(request.getArrivalTime());
        }
        if (request.getBusType() != null) {
            expedition.setBusType(request.getBusType());
        }
        if (request.getPrice() != null) {
            expedition.setPrice(request.getPrice());
        }
        if (request.getTotalSeats() != null) {
            expedition.setTotalSeats(request.getTotalSeats());
        }
    }
    
    // Additional convenience methods using Specifications
    
    /**
     * Get expeditions by departure and arrival cities with pagination
     * 
     * @param departureCity Departure city
     * @param arrivalCity Arrival city
     * @param page Page number
     * @param size Page size
     * @return Paginated list of expeditions
     */
    public BusExpeditionPageResponseDto getBusExpeditionsByRoute(String departureCity, String arrivalCity, 
                                                               Integer page, Integer size) {
        if (page == null || page < 0) page = 0;
        if (size == null || size <= 0 || size > 100) size = 20;
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("departureTime").ascending());
        Specification<BusExpedition> specification = BusExpeditionSpecification.getBusExpeditionsByDepartureAndArrivalCities(
            departureCity, arrivalCity);
        
        Page<BusExpedition> expeditionPage = busExpeditionRepository.findAll(specification, pageable);
        
        List<BusExpeditionResponseDto> expeditionDtos = expeditionPage.getContent().stream()
                .map(BusExpeditionResponseDto::fromBusExpedition)
                .collect(Collectors.toList());
        
        return BusExpeditionPageResponseDto.of(
            expeditionDtos,
            expeditionPage.getNumber(),
            expeditionPage.getTotalPages(),
            expeditionPage.getTotalElements(),
            expeditionPage.getSize(),
            expeditionPage.hasNext(),
            expeditionPage.hasPrevious(),
            expeditionPage.isFirst(),
            expeditionPage.isLast()
        );
    }
    
    /**
     * Get expeditions by bus company with pagination
     * 
     * @param busCompany Bus company name
     * @param page Page number
     * @param size Page size
     * @return Paginated list of expeditions
     */
    public BusExpeditionPageResponseDto getBusExpeditionsByCompany(String busCompany, Integer page, Integer size) {
        if (page == null || page < 0) page = 0;
        if (size == null || size <= 0 || size > 100) size = 20;
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("departureTime").ascending());
        Specification<BusExpedition> specification = BusExpeditionSpecification.getBusExpeditionsByCompany(busCompany);
        
        Page<BusExpedition> expeditionPage = busExpeditionRepository.findAll(specification, pageable);
        
        List<BusExpeditionResponseDto> expeditionDtos = expeditionPage.getContent().stream()
                .map(BusExpeditionResponseDto::fromBusExpedition)
                .collect(Collectors.toList());
        
        return BusExpeditionPageResponseDto.of(
            expeditionDtos,
            expeditionPage.getNumber(),
            expeditionPage.getTotalPages(),
            expeditionPage.getTotalElements(),
            expeditionPage.getSize(),
            expeditionPage.hasNext(),
            expeditionPage.hasPrevious(),
            expeditionPage.isFirst(),
            expeditionPage.isLast()
        );
    }
    
    /**
     * Get expeditions by bus type with pagination
     * 
     * @param busType Bus type
     * @param page Page number
     * @param size Page size
     * @return Paginated list of expeditions
     */
    public BusExpeditionPageResponseDto getBusExpeditionsByType(BusType busType, Integer page, Integer size) {
        if (page == null || page < 0) page = 0;
        if (size == null || size <= 0 || size > 100) size = 20;
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("departureTime").ascending());
        Specification<BusExpedition> specification = BusExpeditionSpecification.getBusExpeditionsByType(busType);
        
        Page<BusExpedition> expeditionPage = busExpeditionRepository.findAll(specification, pageable);
        
        List<BusExpeditionResponseDto> expeditionDtos = expeditionPage.getContent().stream()
                .map(BusExpeditionResponseDto::fromBusExpedition)
                .collect(Collectors.toList());
        
        return BusExpeditionPageResponseDto.of(
            expeditionDtos,
            expeditionPage.getNumber(),
            expeditionPage.getTotalPages(),
            expeditionPage.getTotalElements(),
            expeditionPage.getSize(),
            expeditionPage.hasNext(),
            expeditionPage.hasPrevious(),
            expeditionPage.isFirst(),
            expeditionPage.isLast()
        );
    }
    
    /**
     * Get expeditions within departure time range with pagination
     * 
     * @param startTime Start time
     * @param endTime End time
     * @param page Page number
     * @param size Page size
     * @return Paginated list of expeditions
     */
    public BusExpeditionPageResponseDto getBusExpeditionsByDepartureTimeRange(LocalDateTime startTime, LocalDateTime endTime,
                                                                            Integer page, Integer size) {
        if (page == null || page < 0) page = 0;
        if (size == null || size <= 0 || size > 100) size = 20;
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("departureTime").ascending());
        Specification<BusExpedition> specification = BusExpeditionSpecification.getBusExpeditionsByDepartureTimeRange(startTime, endTime);
        
        Page<BusExpedition> expeditionPage = busExpeditionRepository.findAll(specification, pageable);
        
        List<BusExpeditionResponseDto> expeditionDtos = expeditionPage.getContent().stream()
                .map(BusExpeditionResponseDto::fromBusExpedition)
                .collect(Collectors.toList());
        
        return BusExpeditionPageResponseDto.of(
            expeditionDtos,
            expeditionPage.getNumber(),
            expeditionPage.getTotalPages(),
            expeditionPage.getTotalElements(),
            expeditionPage.getSize(),
            expeditionPage.hasNext(),
            expeditionPage.hasPrevious(),
            expeditionPage.isFirst(),
            expeditionPage.isLast()
        );
    }
    
    /**
     * Get available expeditions with pagination
     * 
     * @param minAvailableSeats Minimum available seats
     * @param page Page number
     * @param size Page size
     * @return Paginated list of available expeditions
     */
    public BusExpeditionPageResponseDto getAvailableBusExpeditions(Integer minAvailableSeats, Integer page, Integer size) {
        if (minAvailableSeats == null) minAvailableSeats = 0;
        if (page == null || page < 0) page = 0;
        if (size == null || size <= 0 || size > 100) size = 20;
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("departureTime").ascending());
        Specification<BusExpedition> specification = BusExpeditionSpecification.getAvailableBusExpeditions(minAvailableSeats);
        
        Page<BusExpedition> expeditionPage = busExpeditionRepository.findAll(specification, pageable);
        
        List<BusExpeditionResponseDto> expeditionDtos = expeditionPage.getContent().stream()
                .map(BusExpeditionResponseDto::fromBusExpedition)
                .collect(Collectors.toList());
        
        return BusExpeditionPageResponseDto.of(
            expeditionDtos,
            expeditionPage.getNumber(),
            expeditionPage.getTotalPages(),
            expeditionPage.getTotalElements(),
            expeditionPage.getSize(),
            expeditionPage.hasNext(),
            expeditionPage.hasPrevious(),
            expeditionPage.isFirst(),
            expeditionPage.isLast()
        );
    }
}
