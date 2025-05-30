package com.biletal.biletalbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusExpeditionPageResponseDto {
    
    private List<BusExpeditionResponseDto> busExpeditions;
    private int currentPage;
    private int totalPages;
    private long totalElements;
    private int pageSize;
    private boolean hasNext;
    private boolean hasPrevious;
    private boolean isFirst;
    private boolean isLast;
    
    public static BusExpeditionPageResponseDto of(List<BusExpeditionResponseDto> busExpeditions, 
                                                 int currentPage, 
                                                 int totalPages, 
                                                 long totalElements, 
                                                 int pageSize, 
                                                 boolean hasNext, 
                                                 boolean hasPrevious,
                                                 boolean isFirst,
                                                 boolean isLast) {
        return new BusExpeditionPageResponseDto(
            busExpeditions, 
            currentPage, 
            totalPages, 
            totalElements, 
            pageSize, 
            hasNext, 
            hasPrevious,
            isFirst,
            isLast
        );
    }
}
