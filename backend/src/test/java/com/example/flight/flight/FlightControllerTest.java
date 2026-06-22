package com.example.flight.flight;

import com.example.flight.auth.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FlightControllerTest {

    @Test
    void delegatesSearchToFlightServiceWithCurrentUser() {
        FlightService flightService = mock(FlightService.class);
        FlightController controller = new FlightController(flightService);
        User user = new User(7L, "alice", "hash", "Alice", LocalDateTime.parse("2026-06-17T09:00:00"));

        when(flightService.search(any(), any(), any(), any(), any())).thenReturn(List.of());

        controller.search("Shanghai", "Beijing", LocalDate.parse("2026-06-19"), "sample", user);

        verify(flightService).search("Shanghai", "Beijing", LocalDate.parse("2026-06-19"), "sample", 7L);
    }

    @Test
    void returnsNotFoundWhenFlightDoesNotExist() {
        FlightService flightService = mock(FlightService.class);
        FlightController controller = new FlightController(flightService);
        when(flightService.findById(99L)).thenReturn(Optional.empty());

        ResponseEntity<Flight> response = controller.findById(99L);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void doesNotLoadPriceHistoryWhenFlightDoesNotExist() {
        FlightService flightService = mock(FlightService.class);
        FlightController controller = new FlightController(flightService);
        when(flightService.findById(99L)).thenReturn(Optional.empty());

        ResponseEntity<List<FlightPriceSnapshot>> response = controller.priceHistory(99L);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }
}
