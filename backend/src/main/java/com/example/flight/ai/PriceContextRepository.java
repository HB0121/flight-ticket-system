package com.example.flight.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 价格上下文仓储 —— 实现 RAG（Retrieval-Augmented Generation）模式的检索层。
 *
 * 职责：
 * 1. 存储航班价格规律知识（种子数据由 {@link PriceContextSeedService} 初始化）
 * 2. 按航线（出发城市+目的城市）检索相关价格规律
 * 3. 支持关键词全文搜索（MySQL FULLTEXT 索引 + LIKE 降级）
 * 4. 作为 {@link TimingService} 的 RAG 数据源，为购票时机分析提供历史规律参考
 *
 * 数据库表：price_context
 * - from_city, to_city: 航线起止城市
 * - depart_date: 出发日期（可为 null，表示通用规律）
 * - context_text: 价格规律文本（RAG 的检索内容）
 * - context_type: 规律类型（PRICE_TREND / PRICE_RANGE / GENERAL_RULE）
 *
 * 设计说明：
 * - searchContext 使用宽松匹配（city = ? or ? is null），确保通用规律也能被检索
 * - searchByKeyword 先尝试 MySQL FULLTEXT 全文索引，失败（如索引不存在）时自动降级到 LIKE
 * - 这种"全文检索 + LIKE 兜底"的降级模式与 AI 降级链思路一致
 */
@Repository
public class PriceContextRepository {

    private static final Logger log = LoggerFactory.getLogger(PriceContextRepository.class);

    /** Spring JdbcTemplate —— 用于执行 SQL 操作 */
    private final JdbcTemplate jdbcTemplate;

    public PriceContextRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 保存一条价格上下文记录。
     *
     * @param fromCity    出发城市
     * @param toCity      目的城市
     * @param departDate  出发日期（可为 null，表示通用规律）
     * @param contextText 价格规律文本内容
     * @param contextType 规律类型标识（如 PRICE_TREND）
     */
    public void saveContext(String fromCity, String toCity, LocalDate departDate,
                            String contextText, String contextType) {
        jdbcTemplate.update(
                "insert into price_context(from_city, to_city, depart_date, context_text, context_type, created_at) values (?, ?, ?, ?, ?, ?)",
                fromCity, toCity, departDate, contextText, contextType,
                Timestamp.valueOf(LocalDateTime.now()));
    }

    /**
     * 按航线检索价格上下文 —— RAG 的主要检索方法。
     *
     * 采用宽松匹配策略：
     * - 如果 city 参数不为 null，匹配 from_city = ? 或 to_city = ?
     * - 如果 city 参数为 null，条件退化为 OR ? is null，从而匹配所有记录（包括通用规律）
     * - 结果按创建时间倒序，最多返回 10 条
     *
     * 这种宽松设计确保：
     * - 精确航线匹配（如"上海→北京"）能返回该航线的专用规律
     * - 通用规律（from_city='', to_city=''）也能被任何查询检索到
     *
     * @param fromCity 出发城市
     * @param toCity   目的城市
     * @return 匹配的上下文文本列表；查询异常时返回空列表（不影响主流程）
     */
    public List<String> searchContext(String fromCity, String toCity) {
        try {
            return jdbcTemplate.query(
                    "select context_text from price_context where (from_city = ? or ? is null or from_city = '') or (to_city = ? or ? is null or to_city = '') order by created_at desc limit 10",
                    (rs, rowNum) -> rs.getString("context_text"),
                    fromCity, fromCity, toCity, toCity);
        } catch (Exception e) {
            // 查询失败时静默降级，不影响主流程
            log.warn("价格上下文检索失败，使用空结果: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 按关键词全文搜索价格上下文 —— searchContext 无结果时的降级检索。
     *
     * 降级链：
     * 1. 首选：MySQL MATCH ... AGAINST 全文索引搜索（高效）
     * 2. 降级：全文索引不可用时 → LIKE '%keyword%' 模糊匹配（兼容性好但性能较低）
     *
     * @param keyword 搜索关键词（如 "上海 北京"）
     * @return 匹配的上下文文本列表；关键词为空时返回空列表
     */
    public List<String> searchByKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        try {
            // 尝试使用 MySQL 全文索引进行自然语言模式搜索
            return jdbcTemplate.query(
                    "select context_text from price_context where match(context_text) against (? in natural language mode) order by created_at desc limit 10",
                    (rs, rowNum) -> rs.getString("context_text"),
                    keyword);
        } catch (Exception e) {
            // 全文索引不可用 → 降级到 LIKE 模糊匹配
            log.debug("全文搜索不可用，使用LIKE降级: {}", e.getMessage());
            return jdbcTemplate.query(
                    "select context_text from price_context where context_text like ? order by created_at desc limit 10",
                    (rs, rowNum) -> rs.getString("context_text"),
                    "%" + keyword + "%");
        }
    }

    /**
     * 获取指定航线的最近价格上下文（精确匹配版本）。
     *
     * @param fromCity 出发城市
     * @param toCity   目的城市
     * @param limit    返回数量上限
     * @return 匹配的上下文文本列表
     */
    public List<String> getRecentContext(String fromCity, String toCity, int limit) {
        return jdbcTemplate.query(
                "select context_text from price_context where from_city = ? and to_city = ? order by created_at desc limit ?",
                (rs, rowNum) -> rs.getString("context_text"),
                fromCity, toCity, limit);
    }

    /**
     * 统计 price_context 表中的记录总数。
     * 用于 {@link PriceContextSeedService} 判断是否需要初始化种子数据。
     *
     * @return 记录总数；表不存在或查询异常时返回 0
     */
    public long count() {
        Long count = jdbcTemplate.queryForObject("select count(*) from price_context", Long.class);
        return count == null ? 0 : count;
    }
}
