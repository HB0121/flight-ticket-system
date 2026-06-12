package com.example.flight.flight;

import java.time.LocalDate;

/**
 * 航班查询条件对象，封装前端传来的多条件搜索参数。
 * 使用 Java Record 保证查询条件不可变，避免在 Repository 层被意外修改。
 *
 * 设计模式：值对象（Value Object）—— 无身份标识，仅通过字段值相等性比较。
 *
 * @param fromCity   出发城市（可选，为空时忽略该条件）
 * @param toCity     到达城市（可选，为空时忽略该条件）
 * @param date       出发日期（可选，查询当天全天航班）
 * @param dataSource 数据来源（可选，"amadeus" 或 "sample"）
 */
public record FlightSearchCriteria(
        String fromCity,
        String toCity,
        LocalDate date,
        String dataSource
) {
    /**
     * 三参数便捷构造器：不限制数据来源，使用默认值 null。
     * 用于不需要按 dataSource 过滤的查询场景。
     */
    public FlightSearchCriteria(String fromCity, String toCity, LocalDate date) {
        this(fromCity, toCity, date, null);
    }
}
