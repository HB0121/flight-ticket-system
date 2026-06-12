package com.example.flight.ai;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 已解析的出行意图 —— 由 {@link TravelIntentParser} 从用户自然语言消息中提取出的
 * 结构化数据。
 *
 * 使用 Java record 实现不可变数据传输对象（DTO）：
 * - fromCity: 出发城市（中文名称，如"上海"）
 * - toCity:   目的城市（中文名称，如"北京"）
 * - date:     出发日期（ISO 格式，如 2026-06-15）
 * - budget:   预算金额（元），如果用户未提供则为 null
 *
 * 所有字段均可为 null —— 上游服务根据实际可用的字段做最大努力匹配。
 */
record ParsedTravelIntent(String fromCity, String toCity, LocalDate date, BigDecimal budget) {
}
