package com.example.flight.ai;

import com.example.flight.flight.Flight;
import com.example.flight.flight.FlightSearchCriteria;
import com.example.flight.flight.FlightSearchPort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class AdviceService {
    private static final List<String> KNOWN_CITIES = List.of(
            "北京", "上海", "广州", "深圳", "成都", "杭州", "西安", "重庆", "武汉", "南京", "厦门", "青岛"
    );
    private static final Pattern DATE_PATTERN = Pattern.compile("(20\\d{2}-\\d{2}-\\d{2})");
    private static final Pattern BUDGET_PATTERN = Pattern.compile("预算\\s*(\\d+)|(?<!\\d)(\\d{3,5})\\s*元");

    private final FlightSearchPort flightSearchPort;

    public AdviceService(FlightSearchPort flightSearchPort) {
        this.flightSearchPort = flightSearchPort;
    }

    public AdviceResponse generate(AdviceRequest request) {
        String message = request == null || request.message() == null ? "" : request.message();
        ParsedIntent intent = parse(message);
        var criteria = new FlightSearchCriteria(intent.fromCity(), intent.toCity(), intent.date());
        List<Flight> candidates = flightSearchPort.search(criteria);

        Optional<Flight> recommended = candidates.stream()
                .filter(flight -> intent.budget() == null || flight.price().compareTo(intent.budget()) <= 0)
                .min(Comparator.comparing(Flight::price).thenComparing(Flight::departTime));

        if (recommended.isEmpty()) {
            String route = routeText(intent);
            return new AdviceResponse("暂未找到" + route + "符合预算的航班，可以放宽预算或更换日期后再试。", null, candidates);
        }

        Flight flight = recommended.get();
        String budgetText = intent.budget() == null ? "当前最低价" : "预算内";
        String summary = "推荐 " + flight.flightNo() + "，" + flight.fromCity() + "到" + flight.toCity()
                + "，" + flight.departTime().toLocalDate() + " " + flight.departTime().toLocalTime()
                + " 起飞，价格 " + flight.price().stripTrailingZeros().toPlainString()
                + " 元，属于" + budgetText + "选择。";
        return new AdviceResponse(summary, flight, candidates);
    }

    private ParsedIntent parse(String message) {
        LocalDate date = DATE_PATTERN.matcher(message).results()
                .map(match -> LocalDate.parse(match.group(1)))
                .findFirst()
                .orElse(null);

        BigDecimal budget = BUDGET_PATTERN.matcher(message).results()
                .map(match -> match.group(1) != null ? match.group(1) : match.group(2))
                .map(BigDecimal::new)
                .findFirst()
                .orElse(null);

        List<String> cities = KNOWN_CITIES.stream()
                .filter(message::contains)
                .sorted(Comparator.comparingInt(message::indexOf))
                .toList();
        String fromCity = cities.size() >= 1 ? cities.get(0) : null;
        String toCity = cities.size() >= 2 ? cities.get(1) : null;
        return new ParsedIntent(fromCity, toCity, date, budget);
    }

    private String routeText(ParsedIntent intent) {
        if (intent.fromCity() != null && intent.toCity() != null) {
            return intent.fromCity() + "到" + intent.toCity();
        }
        return "";
    }

    private record ParsedIntent(String fromCity, String toCity, LocalDate date, BigDecimal budget) {
    }
}
