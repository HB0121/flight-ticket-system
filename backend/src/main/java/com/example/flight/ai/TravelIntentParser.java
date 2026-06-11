package com.example.flight.ai;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

final class TravelIntentParser {
    private static final List<String> KNOWN_CITIES = List.of(
            "北京", "上海", "广州", "深圳", "成都", "杭州", "西安", "重庆", "武汉", "南京", "厦门", "青岛"
    );
    private static final Pattern DATE_PATTERN = Pattern.compile("(20\\d{2}-\\d{2}-\\d{2})");
    private static final Pattern BUDGET_PATTERN = Pattern.compile("预算\\s*(\\d+)|(?<!\\d)(\\d{3,5})\\s*元");

    private TravelIntentParser() {
    }

    static ParsedTravelIntent parse(String message) {
        String safeMessage = message == null ? "" : message;
        LocalDate date = DATE_PATTERN.matcher(safeMessage).results()
                .map(match -> LocalDate.parse(match.group(1)))
                .findFirst()
                .orElse(null);

        BigDecimal budget = BUDGET_PATTERN.matcher(safeMessage).results()
                .map(match -> match.group(1) != null ? match.group(1) : match.group(2))
                .map(BigDecimal::new)
                .findFirst()
                .orElse(null);

        List<String> cities = KNOWN_CITIES.stream()
                .filter(safeMessage::contains)
                .sorted(Comparator.comparingInt(safeMessage::indexOf))
                .toList();
        String fromCity = cities.size() >= 1 ? cities.get(0) : null;
        String toCity = cities.size() >= 2 ? cities.get(1) : null;
        return new ParsedTravelIntent(fromCity, toCity, date, budget);
    }
}
