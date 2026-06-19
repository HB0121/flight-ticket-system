package com.example.flight.ai;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class TravelIntentParser {
    private static final List<String> KNOWN_CITIES = List.of(
            "北京", "上海", "广州", "深圳", "成都", "杭州", "西安", "重庆", "武汉", "南京", "厦门", "青岛",
            "天津", "沈阳", "南宁", "南昌", "乌鲁木齐", "长沙", "昆明"
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

        Route route = resolveRoute(safeMessage);
        TimePreference timePreference = resolveTimePreference(safeMessage);
        return new ParsedTravelIntent(route.fromCity(), route.toCity(), date, budget, timePreference);
    }

    private static Route resolveRoute(String message) {
        for (String fromCity : KNOWN_CITIES) {
            for (String toCity : KNOWN_CITIES) {
                if (!fromCity.equals(toCity) && message.contains(fromCity + "到" + toCity)) {
                    return new Route(fromCity, toCity);
                }
                if (!fromCity.equals(toCity) && message.contains(fromCity + "去" + toCity)) {
                    return new Route(fromCity, toCity);
                }
            }
        }

        String explicitFrom = matchCityAfterKeyword(message, "从");
        if (explicitFrom == null) {
            explicitFrom = matchCityAfterKeyword(message, "由");
        }
        String explicitTo = matchCityAfterKeyword(message, "到");
        if (explicitTo == null) {
            explicitTo = matchCityAfterKeyword(message, "去");
        }

        if (explicitFrom != null || explicitTo != null) {
            return new Route(explicitFrom, explicitTo);
        }

        List<String> cities = KNOWN_CITIES.stream()
                .filter(message::contains)
                .sorted(Comparator.comparingInt(message::indexOf))
                .toList();
        String fromCity = cities.size() >= 1 ? cities.get(0) : null;
        String toCity = cities.size() >= 2 ? cities.get(1) : null;
        return new Route(fromCity, toCity);
    }

    private static String matchCityAfterKeyword(String message, String keyword) {
        int keywordIndex = message.indexOf(keyword);
        if (keywordIndex < 0) {
            return null;
        }

        int closestIndex = Integer.MAX_VALUE;
        String matchedCity = null;
        for (String city : KNOWN_CITIES) {
            int cityIndex = message.indexOf(city, keywordIndex + keyword.length());
            if (cityIndex >= 0 && cityIndex < closestIndex) {
                closestIndex = cityIndex;
                matchedCity = city;
            }
        }
        return matchedCity;
    }

    private static TimePreference resolveTimePreference(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }
        if (containsAny(message, "凌晨", "清晨")) {
            return TimePreference.EARLY_MORNING;
        }
        if (containsAny(message, "上午", "早上", "早晨")) {
            return TimePreference.MORNING;
        }
        if (containsAny(message, "下午", "中午")) {
            return TimePreference.AFTERNOON;
        }
        if (containsAny(message, "晚上", "傍晚", "今晚")) {
            return TimePreference.EVENING;
        }
        return null;
    }

    private static boolean containsAny(String message, String... values) {
        for (String value : values) {
            if (message.contains(value)) {
                return true;
            }
        }
        return false;
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

        Matcher matcher = Pattern.compile("下周([一二三四五六日天])").matcher(message);
        if (matcher.find()) {
            DayOfWeek target = chineseDayToEnum(matcher.group(1));
            return today.with(TemporalAdjusters.next(target));
        }

        matcher = Pattern.compile("本周([一二三四五六日天])").matcher(message);
        if (matcher.find()) {
            DayOfWeek target = chineseDayToEnum(matcher.group(1));
            return today.with(TemporalAdjusters.nextOrSame(target));
        }

        matcher = Pattern.compile("下个月(\\d{1,2})[号日]").matcher(message);
        if (matcher.find()) {
            int day = Integer.parseInt(matcher.group(1));
            LocalDate nextMonth = today.plusMonths(1);
            return nextMonth.withDayOfMonth(Math.min(day, nextMonth.lengthOfMonth()));
        }

        if (message.contains("月底")) {
            return today.withDayOfMonth(today.lengthOfMonth());
        }

        matcher = Pattern.compile("本月(\\d{1,2})[号日]").matcher(message);
        if (matcher.find()) {
            int day = Integer.parseInt(matcher.group(1));
            return today.withDayOfMonth(Math.min(day, today.lengthOfMonth()));
        }

        matcher = Pattern.compile("(\\d{1,2})天后").matcher(message);
        if (matcher.find()) {
            return today.plusDays(Integer.parseInt(matcher.group(1)));
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

    private record Route(String fromCity, String toCity) {
    }
}
