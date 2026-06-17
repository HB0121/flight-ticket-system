package com.example.flight.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 数据库 Schema 懒初始化组件。
 *
 * 在 Spring 容器完全就绪后（ApplicationReadyEvent），通过 ALTER 和 CREATE IF NOT EXISTS
 * 语句对已有或新建的表结构进行增量补齐。所有语句均通过 ignoreFailure 包裹执行，
 * 若字段/表已存在（MySQL 会报 duplicate column/table），静默忽略错误，
 * 保证此组件在首次部署和后续升级时均可安全重复执行。
 *
 * 设计模式：事件监听 + 幂等迁移 —— 监听应用就绪事件，每次启动做一次幂等 DDL。
 */
@Component
public class DatabaseInitializer {
    private final JdbcTemplate jdbcTemplate;

    /** 构造器注入 JdbcTemplate，用于执行原生 SQL */
    public DatabaseInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 监听 Spring 应用就绪事件，在应用可对外提供服务之前完成 Schema 补齐。
     * 所有 DDL 语句独立执行，单条失败不影响后续语句。
     *
     * @see ApplicationReadyEvent
     */
    @EventListener(ApplicationReadyEvent.class)
    public void ensureSecondVersionSchema() {
        // 为 v1 版本的 crawl_job 表补增 v2 所需的列
        ignoreFailure("alter table crawl_job add column source varchar(32) null");
        ignoreFailure("alter table crawl_job add column request_params varchar(500) null");

        // 创建价格快照表（不存在时）：记录每次爬取中每个航班的价格轨迹
        ignoreFailure(
                "create table if not exists flight_price_snapshot (" +
                " id bigint primary key auto_increment," +
                " flight_id bigint not null," +
                " flight_no varchar(20) not null," +
                " from_city varchar(32) not null," +
                " to_city varchar(32) not null," +
                " depart_time datetime not null," +
                " price decimal(10, 2) not null," +
                " seats_left int not null default 0," +
                " data_source varchar(32) not null," +
                " observed_at datetime not null" +
                " )");
        // 为价格快照表创建常用查询索引
        ignoreFailure("create index idx_snapshot_flight on flight_price_snapshot (flight_id, observed_at)");
        ignoreFailure("create index idx_snapshot_route on flight_price_snapshot (from_city, to_city, depart_time)");

        // 创建价格上下文表：存储 AI 生成的价格趋势分析文本
        ignoreFailure(
                "create table if not exists price_context (" +
                " id bigint primary key auto_increment," +
                " from_city varchar(32) not null," +
                " to_city varchar(32) not null," +
                " depart_date date," +
                " context_text text not null," +
                " context_type varchar(32) not null default 'PRICE_TREND'," +
                " created_at datetime not null" +
                " )");

        // 创建对话会话表：支持多轮 AI 对话
        ignoreFailure(
                "create table if not exists conversation_session (" +
                " id varchar(36) primary key," +
                " title varchar(128)," +
                " created_at datetime not null," +
                " updated_at datetime not null" +
                " )");

        // 创建对话消息表：记录每个会话中的用户提问和 AI 回复
        ignoreFailure(
                "create table if not exists conversation_message (" +
                " id bigint primary key auto_increment," +
                " session_id varchar(36) not null," +
                " role varchar(16) not null," +
                " content text not null," +
                " created_at datetime not null" +
                " )");

        // 创建用户表：支持登录注册功能
        ignoreFailure(
                "create table if not exists app_user (" +
                " id bigint primary key auto_increment," +
                " username varchar(32) unique not null," +
                " password varchar(128) not null," +
                " created_at datetime not null" +
                " )");

        // 创建用户令牌表：存储登录后的 session token
        ignoreFailure(
                "create table if not exists user_token (" +
                " id bigint primary key auto_increment," +
                " user_id bigint not null," +
                " token varchar(64) unique not null," +
                " created_at datetime not null," +
                " expires_at datetime not null" +
                " )");
    }

    /**
     * 安全执行一条 DDL 语句，捕获并忽略所有异常。
     * 用于幂等 Schema 迁移：若列/表/索引已存在，MySQL 会报 duplicate 错误，
     * 此处吞掉异常以保证启动不被阻塞。
     *
     * @param sql 待执行的 DDL 语句
     */
    private void ignoreFailure(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception ignored) {
            // 已有 v2 字段/表/索引的实例会在此处进入 catch，安全忽略
        }
    }
}
