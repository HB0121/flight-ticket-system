package com.example.flight.ai;

import java.time.LocalDate;
import java.time.Month;
import java.util.Comparator;
import java.util.List;

/**
 * 节假日接近度计算器 —— 工具类。
 *
 * 职责：
 * 1. 维护 2026-2027 年中国主要节假日的日期范围（元旦、春节、清明、劳动节、端午、中秋、国庆）
 * 2. 计算出发日期距离最近节假日的天数
 * 3. 生成针对性的节假日出行建议文本
 *
 * 设计思路：
 * - 节假日期间票价通常较高，高峰期前后票价也会上涨
 * - 根据距节假日的天数（0天/3天内/7天内/更远）给出分级建议
 * - 所有硬编码的节假日日期集中在 HOLIDAYS 列表中，便于更新
 *
 * 这是一个工具类，不可实例化。
 */
final class HolidayProximityCalculator {

    /** 私有构造函数 —— 工具类不可实例化 */
    private HolidayProximityCalculator() {
    }

    /**
     * 2026-2027 年中国主要节假日日期范围列表。
     *
     * 每个 HolidaySpan 包含：节假日名称、开始日期、结束日期。
     * 日期基于中国国务院发布的放假安排预估。如果实际放假安排有变动，
     * 需要更新此列表中的日期。
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

    /**
     * 计算出发日期距离最近节假日的最短天数。
     *
     * 计算逻辑：
     * - 如果日期落在节假日范围内 → 0 天（正值假期）
     * - 如果日期在节假日前 → 距假期开始的天数
     * - 如果日期在节假日后 → 距假期结束的天数
     * - 取所有节假日中的最小值
     *
     * @param departDate 出发日期
     * @return 距最近节假日的天数；若 departDate 为 null 则返回 Integer.MAX_VALUE
     */
    static int daysToNearestHoliday(LocalDate departDate) {
        if (departDate == null) {
            return Integer.MAX_VALUE;
        }
        return HOLIDAYS.stream()
                .mapToInt(h -> h.daysFrom(departDate))
                .min()
                .orElse(Integer.MAX_VALUE);
    }

    /**
     * 生成节假日出行建议文本。
     *
     * 分级策略：
     * - 0天（正值假期）→ 高峰期，票价高，建议提前购买
     * - 1-3天 → 高峰期前后，票价可能持续上涨
     * - 4-7天 → 高峰期临近，建议尽快购票
     * - 8天以上 → 暂无直接影响
     *
     * @param departDate 出发日期
     * @return 节假日建议文本；如果无法分析则返回空字符串
     */
    static String holidayAdvice(LocalDate departDate) {
        if (departDate == null) {
            return "";
        }
        // 找到距离最近的节假日
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

    /**
     * 节假日时间跨度 —— 内部数据载体。
     *
     * @param name  节假日名称（如"春节"）
     * @param start 假期开始日期（含）
     * @param end   假期结束日期（含）
     */
    private record HolidaySpan(String name, LocalDate start, LocalDate end) {
        /**
         * 计算给定日期距此节假日的最短天数。
         *
         * @param date 要计算的日期
         * @return 0 表示正值假期；正数表示距假期开始或结束的天数
         */
        int daysFrom(LocalDate date) {
            // 日期在假期范围内 → 正值假期
            if (!date.isAfter(end) && !date.isBefore(start)) {
                return 0;
            }
            // 日期在假期开始前 → 距假期开始的天数
            if (date.isBefore(start)) {
                return (int) date.until(start).getDays();
            }
            // 日期在假期结束后 → 距假期结束的天数
            return (int) end.until(date).getDays();
        }

        /** 获取节假日名称（record 的 accessor 方法） */
        public String name() {
            return name;
        }
    }
}
