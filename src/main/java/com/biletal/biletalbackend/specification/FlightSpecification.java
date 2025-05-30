package com.biletal.biletalbackend.specification;

import com.biletal.biletalbackend.dto.FlightSearchRequest;
import com.biletal.biletalbackend.model.Flight;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FlightSpecification {

    public static Specification<Flight> getFlightsBySearchCriteria(FlightSearchRequest searchRequest) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always filter by active flights
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

            // Airline filter (case-insensitive partial match)
            if (searchRequest.getAirline() != null && !searchRequest.getAirline().trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("airline")),
                    "%" + searchRequest.getAirline().toLowerCase() + "%"
                ));
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

            // Only available flights filter
            if (searchRequest.getOnlyAvailable() != null && searchRequest.getOnlyAvailable()) {
                predicates.add(criteriaBuilder.greaterThan(
                    root.get("availableSeats"), 
                    0
                ));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Flight> getActiveFlights() {
        return (root, query, criteriaBuilder) -> 
            criteriaBuilder.isTrue(root.get("isActive"));
    }

    public static Specification<Flight> getFlightsByDepartureAndArrivalCities(String departureCity, String arrivalCity) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            predicates.add(criteriaBuilder.isTrue(root.get("isActive")));
            predicates.add(criteriaBuilder.equal(root.get("departureCity"), departureCity));
            predicates.add(criteriaBuilder.equal(root.get("arrivalCity"), arrivalCity));
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Flight> getFlightsByDepartureTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            predicates.add(criteriaBuilder.isTrue(root.get("isActive")));
            predicates.add(criteriaBuilder.between(root.get("departureTime"), startTime, endTime));
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Flight> getFlightsByAirline(String airline) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            predicates.add(criteriaBuilder.isTrue(root.get("isActive")));
            predicates.add(criteriaBuilder.equal(root.get("airline"), airline));
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Flight> getAvailableFlights(Integer minAvailableSeats) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            predicates.add(criteriaBuilder.isTrue(root.get("isActive")));
            predicates.add(criteriaBuilder.greaterThan(root.get("availableSeats"), minAvailableSeats));
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
