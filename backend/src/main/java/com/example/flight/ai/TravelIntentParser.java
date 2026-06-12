package com.example.flight.ai;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
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
                .orElseGet(() -> resolveRelativeDate(safeMessage));

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

    private static LocalDate resolveRelativeDate(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }
        LocalDate today = LocalDate.now();

        if (message.contains("今天")) return today;
        if (message.contains("明天")) return today.plusDays(1);
        if (message.contains("后天")) return today.plusDays(2);
        if (message.contains("大后天")) return today.plusDays(3);

        // 下周X patterns — next Monday-Sunday
        var m = Pattern.compile("下周([一二三四五六日天])").matcher(message);
        if (m.find()) {
            DayOfWeek target = chineseDayToEnum(m.group(1));
            return today.with(TemporalAdjusters.next(target));
        }

        // 本周X
        m = Pattern.compile("本周([一二三四五六日天])").matcher(message);
        if (m.find()) {
            DayOfWeek target = chineseDayToEnum(m.group(1));
            return today.with(TemporalAdjusters.nextOrSame(target));
        }

        // 下个月N号
        m = Pattern.compile("下个月(\\d{1,2})[号日]").matcher(message);
        if (m.find()) {
            int day = Integer.parseInt(m.group(1));
            LocalDate nextMonth = today.plusMonths(1);
            return nextMonth.withDayOfMonth(Math.min(day, nextMonth.lengthOfMonth()));
        }

        // 月底
        if (message.contains("月底")) {
            return today.withDayOfMonth(today.lengthOfMonth());
        }

        // 本月N号
        m = Pattern.compile("本月(\\d{1,2})[号日]").matcher(message);
        if (m.find()) {
            int day = Integer.parseInt(m.group(1));
            return today.withDayOfMonth(Math.min(day, today.lengthOfMonth()));
        }

        // N天后
        m = Pattern.compile("(\\d{1,2})天后").matcher(message);
        if (m.find()) {
            return today.plusDays(Integer.parseInt(m.group(1)));
        }

        return null;
    }

    private static DayOfWeek chineseDayToEnum(String chinese) {
        if ("一".equals(chinese)) return DayOfWeek.MONDAY;
        if ("二".equals(chinese)) return DayOfWeek.TUESDAY;
        if ("三".equals(chinese)) return DayOfWeek.WEDNESDAY;
        if ("四".equals(chinese)) return DayOfWeek.THURSDAY;
        if ("五".equals(chinese)) return DayOfWeek.FRIDAY;
        if ("六".equals(chinese)) return DayOfWeek.SATURDAY;
        return DayOfWeek.SUNDAY;
    }
}
