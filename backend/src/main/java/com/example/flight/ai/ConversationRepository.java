package com.example.flight.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 对话持久化仓储 —— 基于 JDBC 的会话和消息数据访问层。
 *
 * 管理两张数据库表：
 * - conversation_session: 会话信息（ID、标题、创建/更新时间）
 * - conversation_message: 消息记录（ID、会话ID、角色、内容、创建时间）
 *
 * 关键操作：
 * - createSession: 创建新会话并生成 UUID 作为唯一标识
 * - addMessage: 插入消息并同步更新会话的 updated_at（保持排序时效性）
 * - getMessages: 按创建时间正序加载会话的全部消息（用于构建对话上下文）
 * - listSessions: 按更新时间倒序列出最近的会话（前端会话列表展示）
 * - deleteSession: 级联删除会话及其所有消息（先删消息，再删会话）
 *
 * 所有数据库操作通过 JdbcTemplate 执行，无 ORM 框架依赖，保持简洁。
 */
@Repository
public class ConversationRepository {

    private static final Logger log = LoggerFactory.getLogger(ConversationRepository.class);

    /** Spring JdbcTemplate —— 数据库操作核心 */
    private final JdbcTemplate jdbcTemplate;

    /**
     * ConversationSession 行映射器 —— 将数据库行转换为 ConversationSession 记录。
     * 字段映射：id, title, created_at → createdAt, updated_at → updatedAt
     */
    private final RowMapper<ConversationSession> sessionRowMapper = (rs, rowNum) -> new ConversationSession(
            rs.getString("id"),
            rs.getString("title"),
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("updated_at").toLocalDateTime()
    );

    /**
     * ConversationMessage 行映射器 —— 将数据库行转换为 ConversationMessage 记录。
     * 字段映射：id, session_id → sessionId, role, content, created_at → createdAt
     */
    private final RowMapper<ConversationMessage> messageRowMapper = (rs, rowNum) -> new ConversationMessage(
            rs.getLong("id"),
            rs.getString("session_id"),
            rs.getString("role"),
            rs.getString("content"),
            rs.getTimestamp("created_at").toLocalDateTime()
    );

    public ConversationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 创建新的对话会话。
     *
     * @param title 会话标题
     * @return 新创建的 ConversationSession（包含生成的 UUID 和当前时间戳）
     */
    public ConversationSession createSession(String title) {
        String id = UUID.randomUUID().toString();          // 生成全局唯一标识
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        jdbcTemplate.update(
                "insert into conversation_session(id, title, created_at, updated_at) values (?, ?, ?, ?)",
                id, title, now, now);
        return new ConversationSession(id, title, now.toLocalDateTime(), now.toLocalDateTime());
    }

    /**
     * 向指定会话添加一条消息。
     *
     * 副作用：同步更新 conversation_session 的 updated_at 字段，
     * 确保会话列表按最后活动时间排序时能正确反映最新消息的时间。
     *
     * @param sessionId 会话 ID
     * @param role      消息角色（"user" 或 "assistant"）
     * @param content   消息文本内容
     */
    public void addMessage(String sessionId, String role, String content) {
        // 插入消息记录
        jdbcTemplate.update(
                "insert into conversation_message(session_id, role, content, created_at) values (?, ?, ?, ?)",
                sessionId, role, content, Timestamp.valueOf(LocalDateTime.now()));
        // 更新会话的最后活动时间
        jdbcTemplate.update(
                "update conversation_session set updated_at = ? where id = ?",
                Timestamp.valueOf(LocalDateTime.now()), sessionId);
    }

    /**
     * 获取指定会话的所有消息（按创建时间正序排列）。
     *
     * 正序排列确保消息按对话发生的自然顺序呈现，
     * 用于构建多轮对话的上下文历史。
     *
     * @param sessionId 会话 ID
     * @return 消息列表（按时间正序）；会话不存在时返回空列表
     */
    public List<ConversationMessage> getMessages(String sessionId) {
        return jdbcTemplate.query(
                "select * from conversation_message where session_id = ? order by created_at asc",
                messageRowMapper, sessionId);
    }

    /**
     * 列出最近创建的会话（按更新时间倒序排列）。
     *
     * 倒序排列保证最近活跃的会话排在最前面，符合聊天应用的典型交互模式。
     *
     * @param limit 返回数量上限
     * @return 会话列表（按 updated_at 倒序）
     */
    public List<ConversationSession> listSessions(int limit) {
        return jdbcTemplate.query(
                "select * from conversation_session order by updated_at desc limit ?",
                sessionRowMapper, limit);
    }

    /**
     * 删除指定会话及其所有关联消息（级联删除）。
     *
     * 删除顺序：先删除子表（conversation_message），再删除主表（conversation_session），
     * 避免外键约束错误。如果使用 ON DELETE CASCADE 外键约束，可以只删主表，
     * 但显式先后删除更清晰且不依赖数据库配置。
     *
     * @param sessionId 要删除的会话 ID
     */
    public void deleteSession(String sessionId) {
        // 先删除该会话下的所有消息
        jdbcTemplate.update("delete from conversation_message where session_id = ?", sessionId);
        // 再删除会话本身
        jdbcTemplate.update("delete from conversation_session where id = ?", sessionId);
    }
}
