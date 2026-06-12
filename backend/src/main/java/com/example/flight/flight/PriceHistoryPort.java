package com.example.flight.flight;

import java.util.List;

/**
 * 价格历史端口接口（端口-适配器架构中的"端口"角色）。
 * 定义查询航班价格历史的抽象契约。
 *
 * 当前唯一实现：FlightRepository（JDBC 直连 MySQL flight_price_snapshot 表）。
 * 未来可通过新实现类切换到缓存层、时序数据库等，Service 层无需感知。
 *
 * 设计模式：端口-适配器模式（Ports & Adapters / 六边形架构）
 */
public interface PriceHistoryPort {
    /**
     * 查询指定航班的历史价格快照。
     *
     * @param flightId 航班 ID（flight 表主键）
     * @return 按观测时间升序排列的价格快照列表，可用于绘制价格趋势图
     */
    List<FlightPriceSnapshot> findPriceHistory(Long flightId);
}
