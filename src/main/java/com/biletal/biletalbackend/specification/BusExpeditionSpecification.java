package com.biletal.biletalbackend.specification;

import com.biletal.biletalbackend.dto.BusExpeditionSearchRequest;
import com.biletal.biletalbackend.model.BusExpedition;
import com.biletal.biletalbackend.custenum.BusType;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BusExpeditionSpecification {

    public static Specification<BusExpedition> getBusExpeditionsBySearchCriteria(BusExpeditionSearchRequest searchRequest) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always filter by active bus expeditions
            predicates.add(criteriaBuilder.isTrue(root.get("isActive")));

            // Departure city filter (case-insensitive partial match)
            if (searchRequest.getDepartureCity() != null && !searchRequest.getDepartureCity().trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("departureCity")),
                    "%" + searchRequest.getDepartureCity().toLowerCase() + "%"
                ));
            }

            // Arrival city filter (case-insensitive partial match)
            if (searchRequest.getArrivalCity() != null && !searchRequest.getArrivalCity().trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("arrivalCity")),
                    "%" + searchRequest.getArrivalCity().toLowerCase() + "%"
                ));
            }

            // Bus company filter (case-insensitive partial match)
            if (searchRequest.getBusCompany() != null && !searchRequest.getBusCompany().trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("busCompany")),
                    "%" + searchRequest.getBusCompany().toLowerCase() + "%"
                ));
            }

            // Bus type filter
            if (searchRequest.getBusType() != null) {
                predicates.add(criteriaBuilder.equal(root.get("busType"), searchRequest.getBusType()));
            }

            // Departure date filter (from this date onwards)
            if (searchRequest.getDepartureDate() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    root.get("departureTime"), 
                    searchRequest.getDepartureDate()
                ));
            }

            // Arrival date filter (until this date)
            if (searchRequest.getArrivalDate() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                    root.get("arrivalTime"), 
                    searchRequest.getArrivalDate()
                ));
            }

            // Min price filter
            if (searchRequest.getMinPrice() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    root.get("price"), 
                    searchRequest.getMinPrice()
                ));
            }

            // Max price filter
            if (searchRequest.getMaxPrice() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                    root.get("price"), 
                    searchRequest.getMaxPrice()
                ));
            }

            // Only available bus expeditions filter
            if (searchRequest.getOnlyAvailable() != null && searchRequest.getOnlyAvailable()) {
                predicates.add(criteriaBuilder.greaterThan(
                    root.get("availableSeats"), 
                    0
                ));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<BusExpedition> getActiveBusExpeditions() {
        return (root, query, criteriaBuilder) -> 
            criteriaBuilder.isTrue(root.get("isActive"));
    }

    public static Specification<BusExpedition> getBusExpeditionsByDepartureAndArrivalCities(String departureCity, String arrivalCity) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            predicates.add(criteriaBuilder.isTrue(root.get("isActive")));
            predicates.add(criteriaBuilder.equal(root.get("departureCity"), departureCity));
            predicates.add(criteriaBuilder.equal(root.get("arrivalCity"), arrivalCity));
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<BusExpedition> getBusExpeditionsByDepartureTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            predicates.add(criteriaBuilder.isTrue(root.get("isActive")));
            predicates.add(criteriaBuilder.between(root.get("departureTime"), startTime, endTime));
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<BusExpedition> getBusExpeditionsByCompany(String busCompany) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            predicates.add(criteriaBuilder.isTrue(root.get("isActive")));
            predicates.add(criteriaBuilder.equal(root.get("busCompany"), busCompany));
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<BusExpedition> getBusExpeditionsByType(BusType busType) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            predicates.add(criteriaBuilder.isTrue(root.get("isActive")));
            predicates.add(criteriaBuilder.equal(root.get("busType"), busType));
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<BusExpedition> getAvailableBusExpeditions(Integer minAvailableSeats) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            predicates.add(criteriaBuilder.isTrue(root.get("isActive")));
            predicates.add(criteriaBuilder.greaterThan(root.get("availableSeats"), minAvailableSeats));
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
