package com.example.flight.flight;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 航班数据访问仓库，直接通过 JDBC 模板操作 MySQL 表。
 * 同时实现 FlightSearchPort 和 PriceHistoryPort 两个端口接口，
 * 是端口-适配器架构中的"适配器"角色（JDBC 适配器）。
 *
 * 设计模式与设计决策：
 *   - 仓储模式（Repository）：封装数据访问逻辑
 *   - 不使用 JPA/MyBatis：保持简单透明，方便教学演示 SQL 操作
 *   - RowMapper 行映射器：将 ResultSet 的每一行转换为 Java Record 对象
 *
 * 核心 SQL 模式：动态拼接 WHERE 条件（使用 "where 1=1" 占位技巧），
 * 仅对非空参数追加 AND 条件，避免复杂的分支判断。
 */
@Repository
public class FlightRepository implements FlightSearchPort, PriceHistoryPort {
    private final JdbcTemplate jdbcTemplate;

    /**
     * Flight 表的行映射器。
     * 将数据库行（ResultSet）映射为不可变的 Flight Record 对象。
     * RowMapper 为函数式接口，此处使用 Lambda 表达式实现。
     */
    private final RowMapper<Flight> rowMapper = (rs, rowNum) -> new Flight(
            rs.getLong("id"),
            rs.getString("flight_no"),
            rs.getString("airline_name"),
            rs.getString("from_city"),
            rs.getString("to_city"),
            rs.getString("from_airport"),
            rs.getString("to_airport"),
            rs.getTimestamp("depart_time").toLocalDateTime(), // Timestamp → LocalDateTime 转换
            rs.getTimestamp("arrive_time").toLocalDateTime(),
            rs.getBigDecimal("price"), // 使用 getBigDecimal 保持精度
            rs.getInt("seats_left"),
            rs.getString("data_source"),
            rs.getTimestamp("collected_at").toLocalDateTime()
    );

    /**
     * FlightPriceSnapshot 表的行映射器。
     * 处理 finished_at 可能为 NULL 的情况（爬虫运行中的记录）。
     */
    private final RowMapper<FlightPriceSnapshot> snapshotRowMapper = (rs, rowNum) -> new FlightPriceSnapshot(
            rs.getLong("id"),
            rs.getLong("flight_id"),
            rs.getString("flight_no"),
            rs.getString("from_city"),
            rs.getString("to_city"),
            rs.getTimestamp("depart_time").toLocalDateTime(),
            rs.getInt("price"),
            rs.getInt("seats_left"),
            rs.getString("data_source"),
            rs.getTimestamp("observed_at").toLocalDateTime()
    );

    /** 构造器注入 JdbcTemplate */
    public FlightRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 动态条件查询航班。
     *
     * SQL 构建策略（动态查询模式）：
     * 1. 以 "select * from flight where 1=1" 为骨架，保证后续 AND 语句语法始终正确
     * 2. 逐字段判断：非空时才追加 AND 条件，参数同步添加到 args 列表
     * 3. 日期查询使用半开区间 [date, date+1)，精确匹配当天出发的航班
     * 4. 结果按价格升序、出发时间升序排列（最便宜的优先）
     *
     * @param criteria 搜索条件对象
     * @return 匹配的航班列表
     */
    @Override
    public List<Flight> search(FlightSearchCriteria criteria) {
        var sql = new StringBuilder("select * from flight where 1=1"); // "1=1" 占位技巧：简化 AND 拼接
        var args = new ArrayList<Object>(); // 动态参数列表，长度与 SQL 中 ? 占位符数量一致

        if (StringUtils.hasText(criteria.fromCity())) {
            sql.append(" and from_city = ?");
            args.add(criteria.fromCity());
        }
        if (StringUtils.hasText(criteria.toCity())) {
            sql.append(" and to_city = ?");
            args.add(criteria.toCity());
        }
        if (criteria.date() != null) {
            LocalDate date = criteria.date();
            // 使用半开区间 [date, date+1) 匹配当天全天航班
            sql.append(" and depart_time >= ? and depart_time < ?");
            args.add(Timestamp.valueOf(date.atStartOfDay())); // 当天 00:00:00
            args.add(Timestamp.valueOf(date.plusDays(1).atStartOfDay())); // 次日 00:00:00（不包含）
        }
        if (StringUtils.hasText(criteria.dataSource())) {
            sql.append(" and data_source = ?");
            args.add(criteria.dataSource());
        }

        sql.append(" order by price asc, depart_time asc"); // 默认按价格和时间排序
        return jdbcTemplate.query(sql.toString(), rowMapper, args.toArray());
    }

    /**
     * 按主键 ID 查询单个航班。
     *
     * @param id 航班主键
     * @return Optional 包装的航班，不存在时返回 Optional.empty()
     */
    public Optional<Flight> findById(Long id) {
        var flights = jdbcTemplate.query("select * from flight where id = ?", rowMapper, id);
        return flights.stream().findFirst();
    }

    /**
     * 统计航班总数。
     * 用于前端仪表盘展示数据概览。
     *
     * @return 航班总数，表为空时返回 0
     */
    public long count() {
        Long count = jdbcTemplate.queryForObject("select count(*) from flight", Long.class);
        return count == null ? 0 : count; // 防御性判空
    }

    /**
     * 查询指定航班的历史价格快照（实现 PriceHistoryPort 接口）。
     * 按观测时间升序排列，前端可用于绘制价格趋势折线图。
     *
     * @param flightId 航班 ID
     * @return 价格快照列表
     */
    @Override
    public List<FlightPriceSnapshot> findPriceHistory(Long flightId) {
        return jdbcTemplate.query(
                "select * from flight_price_snapshot where flight_id = ? order by observed_at asc",
                snapshotRowMapper, flightId);
    }
}
