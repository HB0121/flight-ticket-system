package com.example.flight.flight;

import com.example.flight.auth.User;
import com.example.flight.flight.history.SearchHistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/flights")
public class FlightController {

    private static final Logger log = LoggerFactory.getLogger(FlightController.class);

    private final FlightRepository flightRepository;
    private final SearchHistoryService searchHistoryService;

    public FlightController(FlightRepository flightRepository,
                            SearchHistoryService searchHistoryService) {
        this.flightRepository = flightRepository;
        this.searchHistoryService = searchHistoryService;
    }

    @GetMapping
    public List<Flight> search(@RequestParam(required = false) String fromCity,
                               @RequestParam(required = false) String toCity,
                               @RequestParam(required = false)
                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                               @RequestParam(required = false) String dataSource,
                               @RequestAttribute("user") User user) {
        log.debug("flight search: fromCity={}, toCity={}, date={}, dataSource={}", fromCity, toCity, date, dataSource);
        List<Flight> flights = flightRepository.search(new FlightSearchCriteria(fromCity, toCity, date, dataSource));
        if (shouldRecordSearch(fromCity, toCity, date, dataSource)) {
            searchHistoryService.record(user.id(), fromCity, toCity, date, dataSource);
        }
        return flights;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Flight> findById(@PathVariable Long id) {
        return flightRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/price-history")
    public ResponseEntity<List<FlightPriceSnapshot>> priceHistory(@PathVariable Long id) {
        if (flightRepository.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(flightRepository.findPriceHistory(id));
    }

    private boolean shouldRecordSearch(String fromCity, String toCity, LocalDate date, String dataSource) {
        return hasText(fromCity) || hasText(toCity) || date != null || hasText(dataSource);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
