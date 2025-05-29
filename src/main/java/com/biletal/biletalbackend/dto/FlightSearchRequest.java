package com.biletal.biletalbackend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlightSearchRequest {
    
    private String departureCity;
    private String arrivalCity;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime departureDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime arrivalDate;
    
    private String airline;
    private Double minPrice;
    private Double maxPrice;
    private Boolean onlyAvailable = true; // Default to only show available flights
    
    // Pagination parameters
    private Integer page = 0; // Default to first page
    private Integer size = 20; // Default page size
    private String sortBy = "departureTime"; // Default sort field
    private String sortDirection = "ASC"; // Default sort direction (ASC or DESC)
    
    // Helper method to check if any search criteria is provided
    public boolean hasSearchCriteria() {
        return departureCity != null || arrivalCity != null || 
               departureDate != null || arrivalDate != null ||
               airline != null || minPrice != null || maxPrice != null;
    }
    
    // Helper method to validate pagination parameters
    public void validatePagination() {
        if (page == null || page < 0) {
            page = 0;
        }
        if (size == null || size <= 0 || size > 100) {
            size = 20; // Default size with max limit of 100
        }
        if (sortBy == null || sortBy.trim().isEmpty()) {
            sortBy = "departureTime";
        }
        if (sortDirection == null || 
            (!sortDirection.equalsIgnoreCase("ASC") && !sortDirection.equalsIgnoreCase("DESC"))) {
            sortDirection = "ASC";
        }
    }
}
