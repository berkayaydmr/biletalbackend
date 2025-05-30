package com.biletal.biletalbackend.controller;

import com.biletal.biletalbackend.dto.ApiResponseDto;
import com.biletal.biletalbackend.dto.FlightCreateRequest;
import com.biletal.biletalbackend.dto.FlightPageResponseDto;
import com.biletal.biletalbackend.dto.FlightResponseDto;
import com.biletal.biletalbackend.dto.FlightSearchRequest;
import com.biletal.biletalbackend.dto.FlightUpdateRequest;
import com.biletal.biletalbackend.security.JwtService;
import com.biletal.biletalbackend.service.FlightService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/flights")
@Tag(name = "Flight Management", description = "APIs for flight management")
public class FlightController {

    private final FlightService flightService;
    private final JwtService jwtService;

    @Autowired
    public FlightController(FlightService flightService, JwtService jwtService) {
        this.flightService = flightService;
        this.jwtService = jwtService;
    }

    /**
     * Admin endpoint to create a new flight
     */
    @Operation(
        summary = "Create new flight", 
        description = "Creates a new flight (Admin only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Flight created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - admin access required"),
        @ApiResponse(responseCode = "409", description = "Flight number already exists")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<?> createFlight(@Valid @RequestBody FlightCreateRequest request, 
                                        HttpServletRequest httpRequest) {
        try {
            // Verify admin access
            String adminCheckResult = verifyAdminAccess(httpRequest);
            if (adminCheckResult != null) {
                return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponseDto(adminCheckResult, false));
            }

            FlightResponseDto createdFlight = flightService.createFlight(request);
            return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdFlight);

        } catch (RuntimeException e) {
            HttpStatus status = e.getMessage().contains("zaten kullanımda") ? 
                HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST;
            return ResponseEntity
                .status(status)
                .body(new ApiResponseDto(e.getMessage(), false));
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDto("Uçuş oluşturulurken bir hata oluştu.", false));
        }
    }

    /**
     * Admin endpoint to update an existing flight
     */
    @Operation(
        summary = "Update flight", 
        description = "Updates an existing flight (Admin only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Flight updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - admin access required"),
        @ApiResponse(responseCode = "404", description = "Flight not found"),
        @ApiResponse(responseCode = "409", description = "Flight number already exists")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{flightId}")
    public ResponseEntity<?> updateFlight(@PathVariable Long flightId,
                                        @Valid @RequestBody FlightUpdateRequest request,
                                        HttpServletRequest httpRequest) {
        try {
            // Verify admin access
            String adminCheckResult = verifyAdminAccess(httpRequest);
            if (adminCheckResult != null) {
                return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponseDto(adminCheckResult, false));
            }

            FlightResponseDto updatedFlight = flightService.updateFlight(flightId, request);
            return ResponseEntity.ok(updatedFlight);

        } catch (RuntimeException e) {
            HttpStatus status;
            if (e.getMessage().contains("bulunamadı")) {
                status = HttpStatus.NOT_FOUND;
            } else if (e.getMessage().contains("zaten kullanımda")) {
                status = HttpStatus.CONFLICT;
            } else {
                status = HttpStatus.BAD_REQUEST;
            }
            return ResponseEntity
                .status(status)
                .body(new ApiResponseDto(e.getMessage(), false));
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDto("Uçuş güncellenirken bir hata oluştu.", false));
        }
    }

    /**
     * Public endpoint to get flight by ID
     */
    @Operation(
        summary = "Get flight by ID", 
        description = "Retrieves a specific flight by its ID"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Flight found"),
        @ApiResponse(responseCode = "404", description = "Flight not found")
    })
    @GetMapping("/{flightId}")
    public ResponseEntity<?> getFlightById(@PathVariable Long flightId) {
        try {
            FlightResponseDto flight = flightService.getFlightById(flightId);
            return ResponseEntity.ok(flight);
        } catch (RuntimeException e) {
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ApiResponseDto(e.getMessage(), false));
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDto("Uçuş bilgileri alınırken bir hata oluştu.", false));
        }
    }

    /**
     * Public endpoint to get all active flights with pagination
     */
    @Operation(
        summary = "Get all active flights with pagination", 
        description = "Retrieves all active flights with pagination support"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Flights retrieved successfully")
    })
    @GetMapping("/paginated")
    public ResponseEntity<?> getAllActiveFlightsPaginated(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(defaultValue = "departureTime") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDirection) {
        try {
            FlightPageResponseDto flights = flightService.getAllActiveFlights(page, size, sortBy, sortDirection);
            return ResponseEntity.ok(flights);
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDto("Uçuş listesi alınırken bir hata oluştu.", false));
        }
    }
    
    /**
     * Public endpoint to get all active flights (non-paginated for backward compatibility)
     */
    @Operation(
        summary = "Get all active flights", 
        description = "Retrieves all active flights without pagination"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Flights retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<?> getAllActiveFlights() {
        try {
            List<FlightResponseDto> flights = flightService.getAllActiveFlights();
            return ResponseEntity.ok(flights);
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDto("Uçuş listesi alınırken bir hata oluştu.", false));
        }
    }

    /**
     * Public endpoint to search flights with pagination
     */
    @Operation(
        summary = "Search flights", 
        description = "Search flights based on various criteria with pagination support"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Search completed successfully")
    })
    @PostMapping("/search")
    public ResponseEntity<?> searchFlights(@RequestBody FlightSearchRequest searchRequest) {
        try {
            FlightPageResponseDto flights = flightService.searchFlights(searchRequest);
            return ResponseEntity.ok(flights);
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDto("Uçuş arama sırasında bir hata oluştu.", false));
        }
    }
    
    /**
     * Public endpoint to search flights without pagination (for backward compatibility)
     */
    @Operation(
        summary = "Search flights (non-paginated)", 
        description = "Search flights based on various criteria without pagination"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Search completed successfully")
    })
    @PostMapping("/search/all")
    public ResponseEntity<?> searchFlightsNonPaginated(@RequestBody FlightSearchRequest searchRequest) {
        try {
            List<FlightResponseDto> flights = flightService.searchFlightsNonPaginated(searchRequest);
            return ResponseEntity.ok(flights);
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDto("Uçuş arama sırasında bir hata oluştu.", false));
        }
    }

    /**
     * Public endpoint to get flights by route (departure and arrival cities) with pagination
     */
    @Operation(
        summary = "Get flights by route", 
        description = "Get flights filtered by departure and arrival cities with pagination"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Flights retrieved successfully")
    })
    @GetMapping("/route")
    public ResponseEntity<?> getFlightsByRoute(
            @RequestParam String departureCity,
            @RequestParam String arrivalCity,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        try {
            FlightPageResponseDto flights = flightService.getFlightsByRoute(
                departureCity, arrivalCity, page, size);
            return ResponseEntity.ok(flights);
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDto("Rota bazlı uçuş arama sırasında bir hata oluştu.", false));
        }
    }
    
    /**
     * Public endpoint to get flights by airline with pagination
     */
    @Operation(
        summary = "Get flights by airline", 
        description = "Get flights filtered by airline with pagination"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Flights retrieved successfully")
    })
    @GetMapping("/airline")
    public ResponseEntity<?> getFlightsByAirline(
            @RequestParam String airline,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        try {
            FlightPageResponseDto flights = flightService.getFlightsByAirline(airline, page, size);
            return ResponseEntity.ok(flights);
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDto("Havayolu bazlı uçuş arama sırasında bir hata oluştu.", false));
        }
    }
    
    /**
     * Public endpoint to get flights by departure time range with pagination
     */
    @Operation(
        summary = "Get flights by departure time range", 
        description = "Get flights filtered by departure time range with pagination"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Flights retrieved successfully")
    })
    @GetMapping("/time-range")
    public ResponseEntity<?> getFlightsByTimeRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        try {
            FlightPageResponseDto flights = flightService.getFlightsByDepartureTimeRange(
                startTime, endTime, page, size);
            return ResponseEntity.ok(flights);
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDto("Zaman aralığı bazlı uçuş arama sırasında bir hata oluştu.", false));
        }
    }
    
    /**
     * Public endpoint to get available flights with pagination
     */
    @Operation(
        summary = "Get available flights", 
        description = "Get flights with available seats with pagination"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Available flights retrieved successfully")
    })
    @GetMapping("/available")
    public ResponseEntity<?> getAvailableFlights(
            @RequestParam(defaultValue = "0") Integer minSeats,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        try {
            FlightPageResponseDto flights = flightService.getAvailableFlights(minSeats, page, size);
            return ResponseEntity.ok(flights);
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDto("Müsait uçuş arama sırasında bir hata oluştu.", false));
        }
    }

    /**
     * Admin endpoint to delete (deactivate) a flight
     */
    @Operation(
        summary = "Delete flight", 
        description = "Soft deletes a flight by setting it as inactive (Admin only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Flight deleted successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - admin access required"),
        @ApiResponse(responseCode = "404", description = "Flight not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{flightId}")
    public ResponseEntity<?> deleteFlight(@PathVariable Long flightId,
                                        HttpServletRequest httpRequest) {
        try {
            // Verify admin access
            String adminCheckResult = verifyAdminAccess(httpRequest);
            if (adminCheckResult != null) {
                return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponseDto(adminCheckResult, false));
            }

            flightService.deleteFlight(flightId);
            return ResponseEntity.ok(new ApiResponseDto("Uçuş başarıyla silindi.", true));

        } catch (RuntimeException e) {
            HttpStatus status = e.getMessage().contains("bulunamadı") ? 
                HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
            return ResponseEntity
                .status(status)
                .body(new ApiResponseDto(e.getMessage(), false));
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDto("Uçuş silinirken bir hata oluştu.", false));
        }
    }

    // Private helper method to verify admin access
    private String verifyAdminAccess(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        
        if (header == null || !header.startsWith("Bearer ")) {
            return "Yetkilendirme hatası: Token bulunamadı.";
        }
        
        String token = header.substring(7);
        
        try {
            String role = jwtService.extractClaim(token, claims -> claims.get("role", String.class));
            
            if (!"SYSTEM_ADMIN".equals(role)) {
                return "Bu işlem için admin yetkisi gereklidir.";
            }
            
            return null; // No error, admin access verified
        } catch (Exception e) {
            return "Geçersiz token.";
        }
    }
}
