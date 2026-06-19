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

        assertThat(advice.intent().from()).isEqualTo("上海");
        assertThat(advice.intent().to()).isEqualTo("北京");
        assertThat(advice.recommendedFlight()).isNotNull();
        assertThat(advice.recommendedFlight().flightNo()).isEqualTo("MU5101");
        assertThat(advice.summary()).contains("MU5101", "980", "预算内");
        assertThat(searchPort.lastCriteria.fromCity()).isNull();
        assertThat(searchPort.lastCriteria.toCity()).isNull();
        assertThat(searchPort.lastCriteria.date()).isEqualTo(LocalDate.parse("2026-06-19"));
    }

    @Test
    void returnsClearMessageWhenNoFlightMatchesTheRoute() {
        var service = new AdviceService(new StubFlightSearchPort(List.of()));

        var advice = service.generate(new AdviceRequest("2026-06-19 上海到北京，预算1200元"));

        assertThat(advice.recommendedFlight()).isNull();
        assertThat(advice.intent().to()).isEqualTo("北京");
        assertThat(advice.summary()).contains("本地数据库暂无符合条件的航班", "上海", "北京", "先同步航班数据");
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

    @Test
    void filtersOutSoldOutAndOverBudgetFlightsFromCandidates() {
        var searchPort = new StubFlightSearchPort(List.of(
                flight("MU0001", "上海", "北京", "虹桥机场", "首都机场", "680", 0, "2026-06-19T07:30:00", "2026-06-19T09:45:00"),
                flight("MU0002", "上海", "北京", "虹桥机场", "首都机场", "880", 6, "2026-06-19T08:30:00", "2026-06-19T10:45:00"),
                flight("MU0003", "上海", "北京", "浦东机场", "大兴机场", "1380", 8, "2026-06-19T09:30:00", "2026-06-19T11:55:00")
        ));
        var service = new AdviceService(searchPort);

        var advice = service.generate(new AdviceRequest("2026-06-19 上海到北京，预算1200元"));

        assertThat(advice.candidates()).extracting(Flight::flightNo).containsExactly("MU0002");
        assertThat(advice.recommendedFlight().flightNo()).isEqualTo("MU0002");
    }

    @Test
    void filtersByMorningArrivalPreferenceAndLimitsCandidatesToFive() {
        var searchPort = new StubFlightSearchPort(List.of(
                flight("MU1001", "上海", "北京", "虹桥机场", "首都机场", "600", 6, "2026-06-19T06:00:00", "2026-06-19T08:00:00"),
                flight("MU1002", "上海", "北京", "虹桥机场", "首都机场", "650", 6, "2026-06-19T06:30:00", "2026-06-19T08:30:00"),
                flight("MU1003", "上海", "北京", "虹桥机场", "首都机场", "700", 6, "2026-06-19T07:00:00", "2026-06-19T09:00:00"),
                flight("MU1004", "上海", "北京", "虹桥机场", "首都机场", "750", 6, "2026-06-19T07:30:00", "2026-06-19T09:30:00"),
                flight("MU1005", "上海", "北京", "虹桥机场", "首都机场", "800", 6, "2026-06-19T08:00:00", "2026-06-19T10:00:00"),
                flight("MU1006", "上海", "北京", "虹桥机场", "首都机场", "850", 6, "2026-06-19T08:30:00", "2026-06-19T10:30:00"),
                flight("MU1999", "上海", "北京", "浦东机场", "大兴机场", "500", 6, "2026-06-19T12:00:00", "2026-06-19T14:30:00")
        ));
        var service = new AdviceService(searchPort);

        var advice = service.generate(new AdviceRequest("2026-06-19 上海到北京，预算2000元，希望上午到"));

        assertThat(advice.candidates()).hasSize(5);
        assertThat(advice.candidates()).allMatch(flight -> flight.arriveTime().getHour() < 12);
        assertThat(advice.candidates()).extracting(Flight::flightNo)
                .containsExactly("MU1001", "MU1002", "MU1003", "MU1004", "MU1005");
        assertThat(advice.recommendedFlight().flightNo()).isEqualTo("MU1001");
    }

    @Test
    void returnsSyncHintWhenNoLocalFlightsMatch() {
        var service = new AdviceService(new StubFlightSearchPort(List.of()));

        var advice = service.generate(new AdviceRequest("下周五去北京出差，预算1200元，希望上午到"));

        assertThat(advice.candidates()).isEmpty();
        assertThat(advice.recommendedFlight()).isNull();
        assertThat(advice.summary()).contains("本地数据库暂无符合条件的航班", "先同步航班数据");
    }

    @Test
    void fallsBackToRuleBasedAdviceWhenAiClientIsUnavailable() {
        var searchPort = new StubFlightSearchPort(List.of(
                flight("MU2201", "重庆", "北京", "江北机场", "首都机场", "980", 4, "2026-06-19T07:20:00", "2026-06-19T09:40:00"),
                flight("MU2202", "重庆", "北京", "江北机场", "大兴机场", "1080", 9, "2026-06-19T10:30:00", "2026-06-19T13:15:00")
        ));
        var service = new AdviceService(searchPort, (systemPrompt, userPrompt) -> Optional.empty());

        var advice = service.generate(new AdviceRequest("2026-06-19 重庆到北京，预算1200元，希望上午到"));

        assertThat(advice.recommendedFlight().flightNo()).isEqualTo("MU2201");
        assertThat(advice.summary()).contains("MU2201", "980", "预算内", "本地价格快照");
        assertThat(advice.summary()).doesNotContain("MU9999");
    }

    @Test
    void fallsBackToRuleBasedAdviceWhenAiClientThrows() {
        var searchPort = new StubFlightSearchPort(List.of(
                flight("CA3301", "重庆", "北京", "江北机场", "首都机场", "1180", 3, "2026-06-19T09:00:00", "2026-06-19T11:25:00")
        ));
        var service = new AdviceService(searchPort, (systemPrompt, userPrompt) -> {
            throw new IllegalStateException("llm timeout");
        });

        var advice = service.generate(new AdviceRequest("2026-06-19 重庆到北京，预算1500元，希望上午到"));

        assertThat(advice.recommendedFlight().flightNo()).isEqualTo("CA3301");
        assertThat(advice.summary()).contains("CA3301", "1180", "本地价格快照");
    }

    @Test
    void keepsSameDateAndRouteFlightsWhenTimePreferenceHasNoExactMatch() {
        var searchPort = new StubFlightSearchPort(List.of(
                flight("MU8801", "重庆", "北京", "CKG", "PEK", "980", 5, "2026-06-19T13:30:00", "2026-06-19T16:05:00"),
                flight("CA8802", "重庆", "北京", "CKG", "PKX", "1080", 6, "2026-06-19T15:10:00", "2026-06-19T17:45:00")
        ));
        var service = new AdviceService(searchPort);

        var advice = service.generate(new AdviceRequest("2026-06-19 重庆到北京，预算1200元，希望上午出发"));

        assertThat(advice.candidates()).extracting(Flight::flightNo).containsExactly("MU8801", "CA8802");
        assertThat(advice.summary()).contains("暂无完全符合上午出发的航班");
    }

    @Test
    void matchesCityAliasAgainstAirportCodesForMultiAirportCities() {
        var searchPort = new StubFlightSearchPort(List.of(
                flight("MU9901", "未知", "未知", "CKG", "PEK", "920", 5, "2026-06-19T08:30:00", "2026-06-19T10:45:00"),
                flight("CA9902", "未知", "未知", "CKG", "PKX", "1180", 8, "2026-06-19T09:10:00", "2026-06-19T11:35:00"),
                flight("ZH9903", "未知", "未知", "SHE", "TSN", "860", 6, "2026-06-19T07:20:00", "2026-06-19T08:55:00")
        ));
        var service = new AdviceService(searchPort);

        var advice = service.generate(new AdviceRequest("2026-06-19 重庆到北京，预算1500元"));

        assertThat(advice.candidates()).extracting(Flight::flightNo).containsExactly("MU9901", "CA9902");
        assertThat(advice.recommendedFlight().flightNo()).isEqualTo("MU9901");
    }

    private static Flight flight(String flightNo,
                                 String fromCity,
                                 String toCity,
                                 String fromAirport,
                                 String toAirport,
                                 String price) {
        return flight(flightNo, fromCity, toCity, fromAirport, toAirport, price, 12,
                "2026-06-19T08:30:00", "2026-06-19T10:45:00");
    }

    private static Flight flight(String flightNo,
                                 String fromCity,
                                 String toCity,
                                 String fromAirport,
                                 String toAirport,
                                 String price,
                                 int seatsLeft,
                                 String departTime,
                                 String arriveTime) {
        return new Flight(
                1L,
                flightNo,
                flightNo.startsWith("MU") ? "东方航空" : "中国国航",
                fromCity,
                toCity,
                fromAirport,
                toAirport,
                LocalDateTime.parse(departTime),
                LocalDateTime.parse(arriveTime),
                new BigDecimal(price),
                seatsLeft,
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
                    .filter(flight -> criteria.fromCity() == null || flight.fromCity().equals(criteria.fromCity()))
                    .filter(flight -> criteria.toCity() == null || flight.toCity().equals(criteria.toCity()))
                    .filter(flight -> criteria.date() == null || flight.departTime().toLocalDate().equals(criteria.date()))
                    .toList();
        }
    }
}
