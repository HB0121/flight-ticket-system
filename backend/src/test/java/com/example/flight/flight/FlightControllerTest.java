package com.example.flight.flight;

import com.example.flight.auth.User;
import com.example.flight.flight.history.SearchHistoryService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FlightControllerTest {

    @Test
    void recordsSearchHistoryOnlyAfterSuccessfulSearch() {
        FlightRepository flightRepository = mock(FlightRepository.class);
        SearchHistoryService searchHistoryService = mock(SearchHistoryService.class);
        FlightController controller = new FlightController(flightRepository, searchHistoryService);
        User user = new User(7L, "alice", "hash", "Alice", LocalDateTime.parse("2026-06-17T09:00:00"));

        when(flightRepository.search(any())).thenReturn(List.of());

        controller.search("Shanghai", "Beijing", LocalDate.parse("2026-06-19"), "sample", user);

        var inOrder = inOrder(flightRepository, searchHistoryService);
        inOrder.verify(flightRepository).search(any(FlightSearchCriteria.class));
        inOrder.verify(searchHistoryService).record(user.id(), "Shanghai", "Beijing", LocalDate.parse("2026-06-19"), "sample");
    }

    @Test
    void doesNotRecordSearchHistoryWhenSearchFails() {
        FlightRepository flightRepository = mock(FlightRepository.class);
        SearchHistoryService searchHistoryService = mock(SearchHistoryService.class);
        FlightController controller = new FlightController(flightRepository, searchHistoryService);
        User user = new User(7L, "alice", "hash", "Alice", LocalDateTime.parse("2026-06-17T09:00:00"));

        when(flightRepository.search(any())).thenThrow(new RuntimeException("db down"));

        assertThatThrownBy(() -> controller.search("Shanghai", "Beijing", LocalDate.parse("2026-06-19"), "sample", user))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("db down");

        verify(searchHistoryService, never()).record(any(), any(), any(), any(), any());
    }

    @Test
    void skipsRecordingForFullyEmptySearch() {
        FlightRepository flightRepository = mock(FlightRepository.class);
        SearchHistoryService searchHistoryService = mock(SearchHistoryService.class);
        FlightController controller = new FlightController(flightRepository, searchHistoryService);
        User user = new User(7L, "alice", "hash", "Alice", LocalDateTime.parse("2026-06-17T09:00:00"));

        when(flightRepository.search(any())).thenReturn(List.of());

        controller.search("   ", null, null, "", user);

        verify(searchHistoryService, never()).record(any(), any(), any(), any(), any());
    }
}
