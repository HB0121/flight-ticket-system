package com.example.flight.flight;

import java.util.List;

/**
 * 航班搜索端口接口（端口-适配器架构中的"端口"角色）。
 * 定义航班搜索的抽象契约，隔离 Service 层与具体数据访问实现。
 *
 * 当前唯一实现：FlightRepository（JDBC 直连 MySQL）。
 * 设计意图：若未来需要切换到 Elasticsearch、Redis 或其他搜索引擎，
 * 只需新增一个实现类，Service 层无需任何修改。
 *
 * 设计模式：端口-适配器模式（Ports & Adapters / 六边形架构）
 */
public interface FlightSearchPort {
    /**
     * 根据搜索条件查询航班列表。
     *
     * @param criteria 封装的搜索条件（出发城市、到达城市、日期、数据来源）
     * @return 匹配的航班列表，按价格升序、出发时间升序排列
     */
    List<Flight> search(FlightSearchCriteria criteria);
}
