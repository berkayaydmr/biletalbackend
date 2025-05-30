package com.biletal.biletalbackend.controller;

import com.biletal.biletalbackend.dto.ApiResponseDto;
import com.biletal.biletalbackend.dto.BusExpeditionCreateRequest;
import com.biletal.biletalbackend.dto.BusExpeditionPageResponseDto;
import com.biletal.biletalbackend.dto.BusExpeditionResponseDto;
import com.biletal.biletalbackend.dto.BusExpeditionSearchRequest;
import com.biletal.biletalbackend.dto.BusExpeditionUpdateRequest;
import com.biletal.biletalbackend.security.JwtService;
import com.biletal.biletalbackend.service.BusExpeditionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bus-expeditions")
@Tag(name = "Bus Expedition Management", description = "APIs for bus expedition management")
public class BusExpeditionController {

    private final BusExpeditionService busExpeditionService;
    private final JwtService jwtService;

    @Autowired
    public BusExpeditionController(BusExpeditionService busExpeditionService, JwtService jwtService) {
        this.busExpeditionService = busExpeditionService;
        this.jwtService = jwtService;
    }

    /**
     * Admin endpoint to create a new bus expedition
     */
    @Operation(
        summary = "Create new bus expedition", 
        description = "Creates a new bus expedition (Admin only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Bus expedition created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - admin access required"),
        @ApiResponse(responseCode = "409", description = "Expedition number already exists")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<?> createBusExpedition(@Valid @RequestBody BusExpeditionCreateRequest request, 
                                               HttpServletRequest httpRequest) {
        try {
            // Verify admin access
            String adminCheckResult = verifyAdminAccess(httpRequest);
            if (adminCheckResult != null) {
                return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponseDto(adminCheckResult, false));
            }

            BusExpeditionResponseDto createdExpedition = busExpeditionService.createBusExpedition(request);
            return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdExpedition);

        } catch (RuntimeException e) {
            HttpStatus status = e.getMessage().contains("zaten kullanımda") ? 
                HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST;
            return ResponseEntity
                .status(status)
                .body(new ApiResponseDto(e.getMessage(), false));
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDto("Otobüs seferi oluşturulurken bir hata oluştu.", false));
        }
    }

    /**
     * Admin endpoint to update an existing bus expedition
     */
    @Operation(
        summary = "Update bus expedition", 
        description = "Updates an existing bus expedition (Admin only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Bus expedition updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - admin access required"),
        @ApiResponse(responseCode = "404", description = "Bus expedition not found"),
        @ApiResponse(responseCode = "409", description = "Expedition number already exists")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{expeditionId}")
    public ResponseEntity<?> updateBusExpedition(@PathVariable Long expeditionId,
                                               @Valid @RequestBody BusExpeditionUpdateRequest request,
                                               HttpServletRequest httpRequest) {
        try {
            // Verify admin access
            String adminCheckResult = verifyAdminAccess(httpRequest);
            if (adminCheckResult != null) {
                return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponseDto(adminCheckResult, false));
            }

            BusExpeditionResponseDto updatedExpedition = busExpeditionService.updateBusExpedition(expeditionId, request);
            return ResponseEntity.ok(updatedExpedition);

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
                .body(new ApiResponseDto("Otobüs seferi güncellenirken bir hata oluştu.", false));
        }
    }

    /**
     * Public endpoint to get bus expedition by ID
     */
    @Operation(
        summary = "Get bus expedition by ID", 
        description = "Retrieves a specific bus expedition by its ID"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Bus expedition found"),
        @ApiResponse(responseCode = "404", description = "Bus expedition not found")
    })
    @GetMapping("/{expeditionId}")
    public ResponseEntity<?> getBusExpeditionById(@PathVariable Long expeditionId) {
        try {
            BusExpeditionResponseDto expedition = busExpeditionService.getBusExpeditionById(expeditionId);
            return ResponseEntity.ok(expedition);
        } catch (RuntimeException e) {
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ApiResponseDto(e.getMessage(), false));
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDto("Otobüs seferi bilgileri alınırken bir hata oluştu.", false));
        }
    }

    /**
     * Public endpoint to get all active bus expeditions with pagination
     */
    @Operation(
        summary = "Get all active bus expeditions with pagination", 
        description = "Retrieves all active bus expeditions with pagination support"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Bus expeditions retrieved successfully")
    })
    @GetMapping("/paginated")
    public ResponseEntity<?> getAllActiveBusExpeditionsPaginated(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(defaultValue = "departureTime") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDirection) {
        try {
            BusExpeditionPageResponseDto expeditions = busExpeditionService.getAllActiveBusExpeditions(page, size, sortBy, sortDirection);
            return ResponseEntity.ok(expeditions);
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDto("Otobüs seferi listesi alınırken bir hata oluştu.", false));
        }
    }
    
    /**
     * Public endpoint to get all active bus expeditions (non-paginated for backward compatibility)
     */
    @Operation(
        summary = "Get all active bus expeditions", 
        description = "Retrieves all active bus expeditions without pagination"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Bus expeditions retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<?> getAllActiveBusExpeditions() {
        try {
            List<BusExpeditionResponseDto> expeditions = busExpeditionService.getAllActiveBusExpeditions();
            return ResponseEntity.ok(expeditions);
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDto("Otobüs seferi listesi alınırken bir hata oluştu.", false));
        }
    }

    /**
     * Public endpoint to search bus expeditions with pagination
     */
    @Operation(
        summary = "Search bus expeditions", 
        description = "Search bus expeditions based on various criteria with pagination support"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Search completed successfully")
    })
    @PostMapping("/search")
    public ResponseEntity<?> searchBusExpeditions(@RequestBody BusExpeditionSearchRequest searchRequest) {
        try {
            BusExpeditionPageResponseDto expeditions = busExpeditionService.searchBusExpeditions(searchRequest);
            return ResponseEntity.ok(expeditions);
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDto("Otobüs seferi arama sırasında bir hata oluştu.", false));
        }
    }
    
    /**
     * Public endpoint to search bus expeditions without pagination (for backward compatibility)
     */
    @Operation(
        summary = "Search bus expeditions (non-paginated)", 
        description = "Search bus expeditions based on various criteria without pagination"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Search completed successfully")
    })
    @PostMapping("/search/all")
    public ResponseEntity<?> searchBusExpeditionsNonPaginated(@RequestBody BusExpeditionSearchRequest searchRequest) {
        try {
            List<BusExpeditionResponseDto> expeditions = busExpeditionService.searchBusExpeditionsNonPaginated(searchRequest);
            return ResponseEntity.ok(expeditions);
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDto("Otobüs seferi arama sırasında bir hata oluştu.", false));
        }
    }

    /**
     * Public endpoint to get bus expeditions by route (departure and arrival cities) with pagination
     */
    @Operation(
        summary = "Get bus expeditions by route", 
        description = "Get bus expeditions filtered by departure and arrival cities with pagination"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Bus expeditions retrieved successfully")
    })
    @GetMapping("/route")
    public ResponseEntity<?> getBusExpeditionsByRoute(
            @RequestParam String departureCity,
            @RequestParam String arrivalCity,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        try {
            BusExpeditionPageResponseDto expeditions = busExpeditionService.getBusExpeditionsByRoute(
                departureCity, arrivalCity, page, size);
            return ResponseEntity.ok(expeditions);
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDto("Rota bazlı otobüs seferi arama sırasında bir hata oluştu.", false));
        }
    }
    
    /**
     * Public endpoint to get bus expeditions by company with pagination
     */
    @Operation(
        summary = "Get bus expeditions by company", 
        description = "Get bus expeditions filtered by bus company with pagination"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Bus expeditions retrieved successfully")
    })
    @GetMapping("/company")
    public ResponseEntity<?> getBusExpeditionsByCompany(
            @RequestParam String busCompany,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        try {
            BusExpeditionPageResponseDto expeditions = busExpeditionService.getBusExpeditionsByCompany(busCompany, page, size);
            return ResponseEntity.ok(expeditions);
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDto("Şirket bazlı otobüs seferi arama sırasında bir hata oluştu.", false));
        }
    }
    
    /**
     * Public endpoint to get bus expeditions by departure time range with pagination
     */
    @Operation(
        summary = "Get bus expeditions by departure time range", 
        description = "Get bus expeditions filtered by departure time range with pagination"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Bus expeditions retrieved successfully")
    })
    @GetMapping("/time-range")
    public ResponseEntity<?> getBusExpeditionsByTimeRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        try {
            BusExpeditionPageResponseDto expeditions = busExpeditionService.getBusExpeditionsByDepartureTimeRange(
                startTime, endTime, page, size);
            return ResponseEntity.ok(expeditions);
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDto("Zaman aralığı bazlı otobüs seferi arama sırasında bir hata oluştu.", false));
        }
    }
    
    /**
     * Public endpoint to get available bus expeditions with pagination
     */
    @Operation(
        summary = "Get available bus expeditions", 
        description = "Get bus expeditions with available seats with pagination"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Available bus expeditions retrieved successfully")
    })
    @GetMapping("/available")
    public ResponseEntity<?> getAvailableBusExpeditions(
            @RequestParam(defaultValue = "0") Integer minSeats,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        try {
            BusExpeditionPageResponseDto expeditions = busExpeditionService.getAvailableBusExpeditions(minSeats, page, size);
            return ResponseEntity.ok(expeditions);
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDto("Müsait otobüs seferi arama sırasında bir hata oluştu.", false));
        }
    }

    /**
     * Admin endpoint to reserve seats on a bus expedition
     */
    @Operation(
        summary = "Reserve seats on bus expedition", 
        description = "Reserves the specified number of seats on a bus expedition (Admin only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Seats reserved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid seat count or insufficient seats"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - admin access required"),
        @ApiResponse(responseCode = "404", description = "Bus expedition not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{expeditionId}/reserve-seats")
    public ResponseEntity<?> reserveSeats(@PathVariable Long expeditionId,
                                        @RequestParam Integer seatCount,
                                        HttpServletRequest httpRequest) {
        try {
            // Verify admin access
            String adminCheckResult = verifyAdminAccess(httpRequest);
            if (adminCheckResult != null) {
                return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponseDto(adminCheckResult, false));
            }

            BusExpeditionResponseDto updatedExpedition = busExpeditionService.reserveSeats(expeditionId, seatCount);
            return ResponseEntity.ok(updatedExpedition);

        } catch (RuntimeException e) {
            HttpStatus status = e.getMessage().contains("bulunamadı") ? 
                HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
            return ResponseEntity
                .status(status)
                .body(new ApiResponseDto(e.getMessage(), false));
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDto("Koltuk rezervasyonu sırasında bir hata oluştu.", false));
        }
    }

    /**
     * Admin endpoint to release seats on a bus expedition
     */
    @Operation(
        summary = "Release seats on bus expedition", 
        description = "Releases the specified number of seats on a bus expedition (Admin only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Seats released successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid seat count"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - admin access required"),
        @ApiResponse(responseCode = "404", description = "Bus expedition not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{expeditionId}/release-seats")
    public ResponseEntity<?> releaseSeats(@PathVariable Long expeditionId,
                                        @RequestParam Integer seatCount,
                                        HttpServletRequest httpRequest) {
        try {
            // Verify admin access
            String adminCheckResult = verifyAdminAccess(httpRequest);
            if (adminCheckResult != null) {
                return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponseDto(adminCheckResult, false));
            }

            BusExpeditionResponseDto updatedExpedition = busExpeditionService.releaseSeats(expeditionId, seatCount);
            return ResponseEntity.ok(updatedExpedition);

        } catch (RuntimeException e) {
            HttpStatus status = e.getMessage().contains("bulunamadı") ? 
                HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
            return ResponseEntity
                .status(status)
                .body(new ApiResponseDto(e.getMessage(), false));
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDto("Koltuk iadesi sırasında bir hata oluştu.", false));
        }
    }

    /**
     * Admin endpoint to delete (deactivate) a bus expedition
     */
    @Operation(
        summary = "Delete bus expedition", 
        description = "Soft deletes a bus expedition by setting it as inactive (Admin only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Bus expedition deleted successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - admin access required"),
        @ApiResponse(responseCode = "404", description = "Bus expedition not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{expeditionId}")
    public ResponseEntity<?> deleteBusExpedition(@PathVariable Long expeditionId,
                                               HttpServletRequest httpRequest) {
        try {
            // Verify admin access
            String adminCheckResult = verifyAdminAccess(httpRequest);
            if (adminCheckResult != null) {
                return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponseDto(adminCheckResult, false));
            }

            busExpeditionService.deleteBusExpedition(expeditionId);
            return ResponseEntity.ok(new ApiResponseDto("Otobüs seferi başarıyla silindi.", true));

        } catch (RuntimeException e) {
            HttpStatus status = e.getMessage().contains("bulunamadı") ? 
                HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
            return ResponseEntity
                .status(status)
                .body(new ApiResponseDto(e.getMessage(), false));
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDto("Otobüs seferi silinirken bir hata oluştu.", false));
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

    /**
     * Handle validation errors for @Valid annotated request bodies
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponseDto handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        // Create a user-friendly error message
        StringBuilder errorBuilder = new StringBuilder("Doğrulama hataları: ");
        for (Map.Entry<String, String> entry : errors.entrySet()) {
            errorBuilder.append(entry.getKey()).append(" - ").append(entry.getValue()).append("; ");
        }
        
        return new ApiResponseDto(errorBuilder.toString(), false);
    }
}
