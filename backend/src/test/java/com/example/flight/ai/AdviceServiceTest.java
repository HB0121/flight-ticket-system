package com.example.flight.ai;

import com.example.flight.flight.Flight;
import com.example.flight.flight.FlightSearchCriteria;
import com.example.flight.flight.FlightSearchPort;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AdviceServiceTest {
    @Test
    void recommendsCheapestFlightWithinBudgetFromChineseMessage() {
        var searchPort = new StubFlightSearchPort(List.of(
                flight("MU5101", "上海", "北京", "虹桥机场", "首都机场", "980"),
                flight("CA1502", "上海", "北京", "浦东机场", "大兴机场", "1280"),
                flight("ZH9999", "深圳", "北京", "宝安机场", "首都机场", "650")
        ));
        var service = new AdviceService(searchPort);

        var advice = service.generate(new AdviceRequest("2026-06-19 上海到北京，预算1200元"));

        assertThat(advice.recommendedFlight()).isNotNull();
        assertThat(advice.recommendedFlight().flightNo()).isEqualTo("MU5101");
        assertThat(advice.summary()).contains("MU5101", "980", "预算内");
        assertThat(searchPort.lastCriteria.fromCity()).isEqualTo("上海");
        assertThat(searchPort.lastCriteria.toCity()).isEqualTo("北京");
        assertThat(searchPort.lastCriteria.date()).isEqualTo(LocalDate.parse("2026-06-19"));
    }

    @Test
    void returnsClearMessageWhenNoFlightMatchesTheRoute() {
        var service = new AdviceService(new StubFlightSearchPort(List.of()));

        var advice = service.generate(new AdviceRequest("2026-06-19 上海到北京，预算1200元"));

        assertThat(advice.recommendedFlight()).isNull();
        assertThat(advice.summary()).contains("暂未找到", "上海", "北京");
    }

    @Test
    void usesDeepSeekTextWhenAiClientReturnsContent() {
        var searchPort = new StubFlightSearchPort(List.of(
                flight("MU5101", "上海", "北京", "虹桥机场", "首都机场", "980")
        ));
        AiTextClient aiTextClient = (systemPrompt, userPrompt) -> Optional.of("DeepSeek 推荐 MU5101，并建议提前一周购票。");
        var service = new AdviceService(searchPort, aiTextClient);

        var advice = service.generate(new AdviceRequest("2026-06-19 上海到北京，预算1200元"));

        assertThat(advice.summary()).isEqualTo("DeepSeek 推荐 MU5101，并建议提前一周购票。");
        assertThat(advice.recommendedFlight().flightNo()).isEqualTo("MU5101");
    }

    private static Flight flight(String flightNo,
                                 String fromCity,
                                 String toCity,
                                 String fromAirport,
                                 String toAirport,
                                 String price) {
        return new Flight(
                1L,
                flightNo,
                flightNo.startsWith("MU") ? "东方航空" : "中国国航",
                fromCity,
                toCity,
                fromAirport,
                toAirport,
                LocalDateTime.parse("2026-06-19T08:30:00"),
                LocalDateTime.parse("2026-06-19T10:45:00"),
                new BigDecimal(price),
                12,
                "sample",
                LocalDateTime.parse("2026-06-11T10:00:00")
        );
    }

    private static class StubFlightSearchPort implements FlightSearchPort {
        private final List<Flight> flights;
        private FlightSearchCriteria lastCriteria;

        private StubFlightSearchPort(List<Flight> flights) {
            this.flights = flights;
        }

        @Override
        public List<Flight> search(FlightSearchCriteria criteria) {
            this.lastCriteria = criteria;
            return flights.stream()
                    .filter(flight -> flight.fromCity().equals(criteria.fromCity()))
                    .filter(flight -> flight.toCity().equals(criteria.toCity()))
                    .filter(flight -> flight.departTime().toLocalDate().equals(criteria.date()))
                    .toList();
        }
    }
}
