package com.example.flight.ai;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 出行意图解析器 —— 基于正则表达式的中文自然语言理解（NLU）模块。
 *
 * 职责：将用户的自然语言消息（如"帮我查一下后天从上海到北京的机票，预算800元"）
 * 解析为结构化的 {@link ParsedTravelIntent}。
 *
 * 解析策略（按顺序）：
 * 1. 日期提取 —— 优先 ISO 格式（2026-06-15），其次中文相对日期（今天/明天/后天/下周X 等）
 * 2. 预算提取 —— 匹配"预算N"或"N元"模式
 * 3. 城市提取 —— 在已知城市列表中查找，按出现顺序分别作为出发城市和目的城市
 *
 * 这是一个工具类，不可实例化。
 */
final class TravelIntentParser {
    /** 已知的中国城市名称列表 —— 按常见程度排序 */
    private static final List<String> KNOWN_CITIES = List.of(
            "北京", "上海", "广州", "深圳", "成都", "杭州", "西安", "重庆", "武汉", "南京", "厦门", "青岛"
    );
    /** ISO 日期正则：匹配 YYYY-MM-DD 格式（20xx 年份） */
    private static final Pattern DATE_PATTERN = Pattern.compile("(20\\d{2}-\\d{2}-\\d{2})");
    /** 预算正则：匹配"预算 xxx"或独立的"xxx元"模式 */
    private static final Pattern BUDGET_PATTERN = Pattern.compile("预算\\s*(\\d+)|(?<!\\d)(\\d{3,5})\\s*元");

    /** 私有构造函数 —— 工具类不可实例化 */
    private TravelIntentParser() {
    }

    /**
     * 解析用户消息，提取出行意图。
     *
     * 解析流程：
     * 1. 尝试提取 ISO 格式日期（如"2026-06-15"），如果未匹配到则调用 resolveRelativeDate 解析中文相对日期
     * 2. 提取预算金额（正则匹配"预算 800"或"800元"）
     * 3. 在已知城市列表中查找消息中包含的城市，按出现顺序确定出发城市和目的城市
     *
     * @param message 用户输入的原始消息，可为 null
     * @return 解析后的出行意图，未识别到的字段为 null
     */
    static ParsedTravelIntent parse(String message) {
        String safeMessage = message == null ? "" : message;
        // 第1步：提取日期 —— 优先 ISO 格式，其次中文相对日期
        LocalDate date = DATE_PATTERN.matcher(safeMessage).results()
                .map(match -> LocalDate.parse(match.group(1)))
                .findFirst()
                .orElseGet(() -> resolveRelativeDate(safeMessage));

        // 第2步：提取预算金额 —— 正则匹配两种模式
        BigDecimal budget = BUDGET_PATTERN.matcher(safeMessage).results()
                .map(match -> match.group(1) != null ? match.group(1) : match.group(2))
                .map(BigDecimal::new)
                .findFirst()
                .orElse(null);

        // 第3步：提取城市 —— 在已知城市列表中查找并按出现顺序排序
        List<String> cities = KNOWN_CITIES.stream()
                .filter(safeMessage::contains)
                .sorted(Comparator.comparingInt(safeMessage::indexOf))
                .toList();
        // 第一个出现的城市视为出发城市，第二个视为目的城市
        String fromCity = cities.size() >= 1 ? cities.get(0) : null;
        String toCity = cities.size() >= 2 ? cities.get(1) : null;
        return new ParsedTravelIntent(fromCity, toCity, date, budget);
    }

    /**
     * 解析中文相对日期表达式。
     *
     * 支持的表达格式（以当前日期为基准计算）：
     * - "今天" / "明天" / "后天" / "大后天" —— 偏移 0/1/2/3 天
     * - "下周X"（如"下周三"）—— 下个该星期几
     * - "本周X"（如"本周五"）—— 本周或当天（如果已经是该星期几）
     * - "下个月N号/日" —— 下个月的第 N 天
     * - "月底" —— 本月最后一天
     * - "本月N号/日" —— 本月的第 N 天
     * - "N天后" —— 当前日期偏移 N 天
     *
     * @param message 用户原始消息
     * @return 解析出的日期，无法识别时返回 null
     */
    private static LocalDate resolveRelativeDate(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }
        LocalDate today = LocalDate.now();

        // 简单偏移日期：今天 / 明天 / 后天 / 大后天
        if (message.contains("今天")) return today;
        if (message.contains("明天")) return today.plusDays(1);
        if (message.contains("后天")) return today.plusDays(2);
        if (message.contains("大后天")) return today.plusDays(3);

        // 下周X 模式 —— 下周一至下周日（"日"和"天"都表示周日）
        var m = Pattern.compile("下周([一二三四五六日天])").matcher(message);
        if (m.find()) {
            DayOfWeek target = chineseDayToEnum(m.group(1));
            // TemporalAdjusters.next() 返回下个该星期几（不含当天）
            return today.with(TemporalAdjusters.next(target));
        }

        // 本周X 模式 —— 本周一至本周日
        m = Pattern.compile("本周([一二三四五六日天])").matcher(message);
        if (m.find()) {
            DayOfWeek target = chineseDayToEnum(m.group(1));
            // TemporalAdjusters.nextOrSame() 返回当天（如果匹配）或下个该星期几
            return today.with(TemporalAdjusters.nextOrSame(target));
        }

        // 下个月N号 模式 —— 如"下个月15号"
        m = Pattern.compile("下个月(\\d{1,2})[号日]").matcher(message);
        if (m.find()) {
            int day = Integer.parseInt(m.group(1));
            LocalDate nextMonth = today.plusMonths(1);
            // Math.min 防止 day 超过该月最大天数（如31号在2月不存在）
            return nextMonth.withDayOfMonth(Math.min(day, nextMonth.lengthOfMonth()));
        }

        // 月底 模式 —— 本月最后一天
        if (message.contains("月底")) {
            return today.withDayOfMonth(today.lengthOfMonth());
        }

        // 本月N号 模式 —— 如"本月20号"
        m = Pattern.compile("本月(\\d{1,2})[号日]").matcher(message);
        if (m.find()) {
            int day = Integer.parseInt(m.group(1));
            return today.withDayOfMonth(Math.min(day, today.lengthOfMonth()));
        }

        // N天后 模式 —— 如"3天后"
        m = Pattern.compile("(\\d{1,2})天后").matcher(message);
        if (m.find()) {
            return today.plusDays(Integer.parseInt(m.group(1)));
        }

        return null;
    }

    /**
     * 将中文星期数字转换为 Java DayOfWeek 枚举。
     *
     * @param chinese 中文星期表示："一"~"六"、"日"或"天"
     * @return 对应的 DayOfWeek 枚举值（"日"/"天" 映射为 SUNDAY）
     */
    private static DayOfWeek chineseDayToEnum(String chinese) {
        if ("一".equals(chinese)) return DayOfWeek.MONDAY;
        if ("二".equals(chinese)) return DayOfWeek.TUESDAY;
        if ("三".equals(chinese)) return DayOfWeek.WEDNESDAY;
        if ("四".equals(chinese)) return DayOfWeek.THURSDAY;
        if ("五".equals(chinese)) return DayOfWeek.FRIDAY;
        if ("六".equals(chinese)) return DayOfWeek.SATURDAY;
        return DayOfWeek.SUNDAY; // "日" 或 "天"
    }
}
