package com.example.flight.flight;

import com.example.flight.flight.history.SearchHistoryService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FlightServiceTest {

    @Test
    void recordsSearchHistoryOnlyAfterSuccessfulSearch() {
        FlightRepository flightRepository = mock(FlightRepository.class);
        SearchHistoryService searchHistoryService = mock(SearchHistoryService.class);
        FlightService service = new FlightService(flightRepository, searchHistoryService);

        when(flightRepository.search(any())).thenReturn(List.of());

        service.search("Shanghai", "Beijing", LocalDate.parse("2026-06-19"), "sample", 7L);

        var inOrder = inOrder(flightRepository, searchHistoryService);
        inOrder.verify(flightRepository).search(any(FlightSearchCriteria.class));
        inOrder.verify(searchHistoryService).record(7L, "Shanghai", "Beijing", LocalDate.parse("2026-06-19"), "sample");
    }

    @Test
    void doesNotRecordSearchHistoryWhenSearchFails() {
        FlightRepository flightRepository = mock(FlightRepository.class);
        SearchHistoryService searchHistoryService = mock(SearchHistoryService.class);
        FlightService service = new FlightService(flightRepository, searchHistoryService);

        when(flightRepository.search(any())).thenThrow(new RuntimeException("db down"));

        assertThatThrownBy(() -> service.search("Shanghai", "Beijing", LocalDate.parse("2026-06-19"), "sample", 7L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("db down");

        verify(searchHistoryService, never()).record(any(), any(), any(), any(), any());
    }

    @Test
    void skipsRecordingForFullyEmptySearch() {
        FlightRepository flightRepository = mock(FlightRepository.class);
        SearchHistoryService searchHistoryService = mock(SearchHistoryService.class);
        FlightService service = new FlightService(flightRepository, searchHistoryService);

        when(flightRepository.search(any())).thenReturn(List.of());

        service.search("   ", null, null, "", 7L);

        verify(searchHistoryService, never()).record(any(), any(), any(), any(), any());
    }
}
