package com.example.flight.ai;

import com.example.flight.flight.Flight;
import com.example.flight.flight.FlightPriceSnapshot;
import com.example.flight.flight.FlightSearchCriteria;
import com.example.flight.flight.FlightSearchPort;
import com.example.flight.flight.PriceHistoryPort;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class TimingServiceTest {
    @Test
    void buildsLocalTimingReportFromPriceHistoryWhenAiUnavailable() {
        var service = new TimingService(
                new StubFlightSearchPort(List.of(flight("MU5101", "980"))),
                flightId -> List.of(
                        snapshot(1L, 1020, "2026-06-11T10:00:00"),
                        snapshot(1L, 980, "2026-06-11T11:00:00")
                ),
                (systemPrompt, userPrompt) -> Optional.empty()
        );

        var response = service.analyze(new TimingRequest("2026-06-19 上海到北京，预算1200元"));

        assertThat(response.summary()).contains("本地分析", "MU5101", "下降");
        assertThat(response.riskLevel()).isEqualTo("LOW");
        assertThat(response.history()).hasSize(2);
    }

    private static Flight flight(String flightNo, String price) {
        return new Flight(
                1L,
                flightNo,
                "东方航空",
                "上海",
                "北京",
                "虹桥机场",
                "首都机场",
                LocalDateTime.parse("2026-06-19T08:30:00"),
                LocalDateTime.parse("2026-06-19T10:45:00"),
                new BigDecimal(price),
                12,
                "amadeus",
                LocalDateTime.parse("2026-06-11T10:00:00")
        );
    }

    private static FlightPriceSnapshot snapshot(Long flightId, int price, String observedAt) {
        return new FlightPriceSnapshot(
                1L,
                flightId,
                "MU5101",
                "上海",
                "北京",
                LocalDateTime.parse("2026-06-19T08:30:00"),
                price,
                12,
                "amadeus",
                LocalDateTime.parse(observedAt)
        );
    }

    private static class StubFlightSearchPort implements FlightSearchPort {
        private final List<Flight> flights;

        private StubFlightSearchPort(List<Flight> flights) {
            this.flights = flights;
        }

        @Override
        public List<Flight> search(FlightSearchCriteria criteria) {
            assertThat(criteria.fromCity()).isEqualTo("上海");
            assertThat(criteria.toCity()).isEqualTo("北京");
            assertThat(criteria.date()).isEqualTo(LocalDate.parse("2026-06-19"));
            return flights;
        }
    }
}
