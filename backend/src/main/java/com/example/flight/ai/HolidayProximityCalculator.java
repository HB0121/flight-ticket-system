package com.example.flight.ai;

import java.time.LocalDate;
import java.time.Month;
import java.util.Comparator;
import java.util.List;

final class HolidayProximityCalculator {

    private HolidayProximityCalculator() {
    }

    /**
     * Major Chinese holidays for 2026–2027 with date ranges.
     */
    private static final List<HolidaySpan> HOLIDAYS = List.of(
            new HolidaySpan("元旦", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 3)),
            new HolidaySpan("春节", LocalDate.of(2026, 2, 17), LocalDate.of(2026, 2, 23)),
            new HolidaySpan("清明节", LocalDate.of(2026, 4, 5), LocalDate.of(2026, 4, 7)),
            new HolidaySpan("劳动节", LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 5)),
            new HolidaySpan("端午节", LocalDate.of(2026, 5, 31), LocalDate.of(2026, 6, 2)),
            new HolidaySpan("中秋节", LocalDate.of(2026, 9, 25), LocalDate.of(2026, 9, 27)),
            new HolidaySpan("国庆节", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 7)),
            new HolidaySpan("元旦", LocalDate.of(2027, 1, 1), LocalDate.of(2027, 1, 3)),
            new HolidaySpan("春节", LocalDate.of(2027, 2, 6), LocalDate.of(2027, 2, 12)),
            new HolidaySpan("清明节", LocalDate.of(2027, 4, 5), LocalDate.of(2027, 4, 7)),
            new HolidaySpan("劳动节", LocalDate.of(2027, 5, 1), LocalDate.of(2027, 5, 5)),
            new HolidaySpan("端午节", LocalDate.of(2027, 5, 19), LocalDate.of(2027, 5, 21)),
            new HolidaySpan("中秋节", LocalDate.of(2027, 9, 15), LocalDate.of(2027, 9, 17)),
            new HolidaySpan("国庆节", LocalDate.of(2027, 10, 1), LocalDate.of(2027, 10, 7))
    );

    static int daysToNearestHoliday(LocalDate departDate) {
        if (departDate == null) {
            return Integer.MAX_VALUE;
        }
        return HOLIDAYS.stream()
                .mapToInt(h -> h.daysFrom(departDate))
                .min()
                .orElse(Integer.MAX_VALUE);
    }

    static String holidayAdvice(LocalDate departDate) {
        if (departDate == null) {
            return "";
        }
        HolidaySpan nearest = HOLIDAYS.stream()
                .min(Comparator.comparingInt(h -> h.daysFrom(departDate)))
                .orElse(null);
        if (nearest == null) {
            return "";
        }
        int days = nearest.daysFrom(departDate);
        if (days == 0) {
            return "出发日期正值" + nearest.name() + "假期，属于出行高峰期，票价较高，建议提前购买。";
        }
        if (days <= 3) {
            return "距离" + nearest.name() + "仅剩" + days + "天，属于出行高峰期前后，票价可能持续上涨。";
        }
        if (days <= 7) {
            return "距离" + nearest.name() + "还有" + days + "天，高峰期临近，建议尽快购票。";
        }
        return "最近的主要节假日是" + nearest.name() + "（距离" + days + "天），暂无直接影响。";
    }

    private record HolidaySpan(String name, LocalDate start, LocalDate end) {
        int daysFrom(LocalDate date) {
            if (!date.isAfter(end) && !date.isBefore(start)) {
                return 0; // during holiday
            }
            if (date.isBefore(start)) {
                return (int) date.until(start).getDays();
            }
            return (int) end.until(date).getDays();
        }

        public String name() {
            return name;
        }
    }
}
