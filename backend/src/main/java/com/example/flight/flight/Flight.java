package com.example.flight.flight;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 航班领域实体，对应数据库 flight 表的一行记录。
 * 使用 Java Record 实现不可变数据对象（DDD 值对象风格），
 * 所有字段为 final，构造后不可修改，安全地在各层之间传递。
 *
 * @param id            主键 ID（数据库自增）
 * @param flightNo      航班号（如 CA1234），与 departTime、dataSource 组成业务唯一键
 * @param airlineName   航空公司名称（如 "中国国际航空"）
 * @param fromCity      出发城市中文名（如 "上海"）
 * @param toCity        到达城市中文名（如 "北京"）
 * @param fromAirport   出发机场名（如 "浦东国际机场"）
 * @param toAirport     到达机场名（如 "首都国际机场"）
 * @param departTime    计划出发时间
 * @param arriveTime    计划到达时间
 * @param price         当前票价，使用 BigDecimal 避免浮点精度问题
 * @param seatsLeft     剩余座位数
 * @param dataSource    数据来源标识（"amadeus" 真实数据 / "sample" 静态样本）
 * @param collectedAt   数据采集时间（爬虫写入时间）
 */
public record Flight(
        Long id,
        String flightNo,
        String airlineName,
        String fromCity,
        String toCity,
        String fromAirport,
        String toAirport,
        LocalDateTime departTime,
        LocalDateTime arriveTime,
        BigDecimal price,
        Integer seatsLeft,
        String dataSource,
        LocalDateTime collectedAt
) {
}
