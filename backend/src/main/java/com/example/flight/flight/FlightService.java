package com.example.flight.flight;

import com.example.flight.flight.history.SearchHistoryService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class FlightService {

    private final FlightRepository flightRepository;
    private final SearchHistoryService searchHistoryService;

    public FlightService(FlightRepository flightRepository,
                         SearchHistoryService searchHistoryService) {
        this.flightRepository = flightRepository;
        this.searchHistoryService = searchHistoryService;
    }

    public List<Flight> search(String fromCity,
                               String toCity,
                               LocalDate date,
                               String dataSource,
                               Long userId) {
        List<Flight> flights = flightRepository.search(new FlightSearchCriteria(fromCity, toCity, date, dataSource));
        if (shouldRecordSearch(fromCity, toCity, date, dataSource)) {
            searchHistoryService.record(userId, fromCity, toCity, date, dataSource);
        }
        return flights;
    }

    public Optional<Flight> findById(Long id) {
        return flightRepository.findById(id);
    }

    public List<FlightPriceSnapshot> findPriceHistory(Long flightId) {
        return flightRepository.findPriceHistory(flightId);
    }

    private boolean shouldRecordSearch(String fromCity, String toCity, LocalDate date, String dataSource) {
        return hasText(fromCity) || hasText(toCity) || date != null || hasText(dataSource);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
