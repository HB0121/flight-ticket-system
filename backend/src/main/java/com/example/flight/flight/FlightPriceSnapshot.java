package com.example.flight.flight;

import java.time.LocalDateTime;

/**
 * 航班价格快照实体，对应数据库 flight_price_snapshot 表的一行记录。
 * 每次爬虫执行时，为每个航班记录一条价格快照，形成 append-only 价格历史。
 *
 * 价格字段使用 Integer（而非 BigDecimal）是因为爬虫端存储时已转换为整数分/元单位，
 * 与 Flight 实体的 BigDecimal price 在精度策略上保持一致。
 *
 * @param id         快照主键（自增）
 * @param flightId   关联的航班 ID（flight 表外键）
 * @param flightNo   航班号（冗余存储，方便独立查询）
 * @param fromCity   出发城市（冗余存储，方便按航线聚合分析）
 * @param toCity     到达城市
 * @param departTime 计划出发时间
 * @param price      此时观测到的票价
 * @param seatsLeft  此时观测到的剩余座位数
 * @param dataSource 数据来源标识
 * @param observedAt 观测时间（爬虫写入该快照的时间）
 */
public record FlightPriceSnapshot(
        Long id,
        Long flightId,
        String flightNo,
        String fromCity,
        String toCity,
        LocalDateTime departTime,
        Integer price,
        Integer seatsLeft,
        String dataSource,
        LocalDateTime observedAt
) {
}
