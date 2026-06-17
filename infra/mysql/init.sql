-- ============================================================================
-- 机票抓取与自动更新系统 · MySQL 初始化脚本
--
-- 执行方式：
--   mysql -u root -p123456 flight_demo < infra/mysql/init.sql
--
-- 包含：建表语句（共 8 张表）+ 种子数据（20 条航班 + 60 条价格快照）
-- ============================================================================

-- ============================================================================
-- 1. flight —— 航班当前状态表（核心业务表）
--
-- 存储每条航班的最新快照。爬虫每次采集时通过 upsert（唯一键冲突则更新）
-- 写入：价格、余票、采集时间都会被覆盖为最新值。
-- ============================================================================
create table if not exists flight (
    id              bigint primary key auto_increment,
    flight_no       varchar(20)  not null,              -- 航班号，如 MU5101
    airline_name    varchar(64)  not null,              -- 航司名称，如 东方航空
    from_city       varchar(32)  not null,              -- 出发城市（中文），如 上海
    to_city         varchar(32)  not null,              -- 到达城市（中文），如 北京
    from_airport    varchar(64)  not null,              -- 出发机场，如 虹桥机场
    to_airport      varchar(64)  not null,              -- 到达机场，如 首都机场
    depart_time     datetime     not null,              -- 起飞时间
    arrive_time     datetime     not null,              -- 到达时间
    price           decimal(10,2) not null,             -- 当前票价（元）
    seats_left      int          not null default 0,     -- 剩余座位数
    data_source     varchar(32)  not null,              -- 数据来源：sample / amadeus
    collected_at    datetime     not null,              -- 最近一次采集时间
    unique key uk_flight_source (flight_no, depart_time, data_source),
    key idx_route_date (from_city, to_city, depart_time),
    key idx_price (price)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

-- ============================================================================
-- 2. flight_price_snapshot —— 价格历史快照表
--
-- 每次爬虫采集时，除了更新 flight 表，还会向本表追加一行记录。
-- 通过多次采集积累的价格数据，用于前端"价格趋势图"和 AI 购票时机分析。
-- ============================================================================
create table if not exists flight_price_snapshot (
    id              bigint primary key auto_increment,
    flight_id       bigint       not null,              -- 关联 flight.id
    flight_no       varchar(20)  not null,              -- 航班号（冗余，方便查询）
    from_city       varchar(32)  not null,              -- 出发城市（冗余）
    to_city         varchar(32)  not null,              -- 到达城市（冗余）
    depart_time     datetime     not null,              -- 起飞时间
    price           decimal(10,2) not null,             -- 本次观测到的票价
    seats_left      int          not null default 0,     -- 本次观测到的余票
    data_source     varchar(32)  not null,              -- 数据来源
    observed_at     datetime     not null,              -- 观测时间（本次采集时间）
    key idx_snapshot_flight (flight_id, observed_at),
    key idx_snapshot_route (from_city, to_city, depart_time)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

-- ============================================================================
-- 3. crawl_job —— 爬虫任务审计表
--
-- 每次触发爬虫（无论是手动还是 API 触发），都会在此表创建一条任务记录。
-- 记录爬虫的执行状态（RUNNING → SUCCESS/FAILED）、成功/失败数量、错误信息。
-- ============================================================================
create table if not exists crawl_job (
    id              bigint primary key auto_increment,
    status          varchar(16)  not null,              -- RUNNING / SUCCESS / FAILED
    started_at      datetime     not null,              -- 任务开始时间
    finished_at     datetime     null,                  -- 任务结束时间
    success_count   int          not null default 0,     -- 成功采集的航班数
    failed_count    int          not null default 0,     -- 采集失败的航班数
    error_message   varchar(1000) null,                 -- 错误详情（失败时）
    source          varchar(32)  null,                  -- 数据来源：sample / amadeus
    request_params  varchar(500) null,                  -- 请求参数摘要
    key idx_started_at (started_at)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

-- ============================================================================
-- 4. price_context —— 价格规律知识库（RAG 检索数据源）
--
-- 存储预定义的机票价格规律文本，供 AI 购票时机分析时检索。
-- 包含各航线的价格趋势、价格区间参考、通用购票规律等。
-- 种子数据由后端 PriceContextSeedService 在首次启动时自动写入。
-- ============================================================================
create table if not exists price_context (
    id              bigint primary key auto_increment,
    from_city       varchar(32)  not null,              -- 出发城市（空字符串=通用规律）
    to_city         varchar(32)  not null,              -- 到达城市（空字符串=通用规律）
    depart_date     date,                               -- 出发日期（null=通用规律）
    context_text    text         not null,              -- 价格规律文本（检索内容）
    context_type    varchar(32)  not null default 'PRICE_TREND',  -- 类型：PRICE_TREND / PRICE_RANGE / GENERAL_RULE
    created_at      datetime     not null,
    key idx_price_context_route (from_city, to_city),
    fulltext key ft_price_context (context_text)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

-- ============================================================================
-- 5. conversation_session —— AI 对话会话表
--
-- 每个会话代表用户与 AI 的一次多轮对话，包含会话标题和创建/更新时间。
-- ============================================================================
create table if not exists conversation_session (
    id              varchar(36)  primary key,           -- UUID，由后端生成
    title           varchar(128),                       -- 会话标题
    created_at      datetime     not null,
    updated_at      datetime     not null               -- 最后一次发消息的时间
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

-- ============================================================================
-- 6. conversation_message —— AI 对话消息表
--
-- 存储每次对话中的用户消息和 AI 回复，按时间正序排列构成对话历史。
-- ============================================================================
create table if not exists conversation_message (
    id              bigint primary key auto_increment,
    session_id      varchar(36)  not null,              -- 关联 conversation_session.id
    role            varchar(16)  not null,              -- user / assistant
    content         text         not null,              -- 消息文本
    created_at      datetime     not null,
    key idx_cm_session (session_id, created_at)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

-- ============================================================================
-- 7. app_user —— 用户表
--
-- 存储系统登录用户。密码使用 BCrypt 哈希，永不存储明文。
-- 应用首次启动时自动创建 admin/admin123 默认账号。
-- ============================================================================
create table if not exists app_user (
    id              bigint primary key auto_increment,
    username        varchar(32)  unique not null,       -- 用户名，全局唯一
    password        varchar(128) not null,              -- BCrypt 哈希后的密码
    created_at      datetime     not null
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

-- ============================================================================
-- 8. user_token —— 用户登录令牌表
--
-- 用户每次登录成功后生成一个 UUID 令牌，有效期 7 天。
-- 令牌失效方式：① 过期（expires_at < now）② 用户主动登出（删除记录）
-- ============================================================================
create table if not exists user_token (
    id              bigint primary key auto_increment,
    user_id         bigint       not null,              -- 关联 app_user.id
    token           varchar(64)  unique not null,       -- UUID 令牌字符串
    created_at      datetime     not null,
    expires_at      datetime     not null,              -- 过期时间（创建时间 + 7 天）
    key idx_token (token),
    key idx_user (user_id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

-- ============================================================================
-- 种子数据 —— 20 条样例航班 + 60 条价格快照
-- 确保系统在无爬虫数据时也能正常演示
-- ============================================================================

-- ----- 航班数据 -----
-- 上海 → 北京 (4班)
insert into flight(flight_no, airline_name, from_city, to_city, from_airport, to_airport, depart_time, arrive_time, price, seats_left, data_source, collected_at)
values ('MU5101', '东方航空', '上海', '北京', '虹桥机场', '首都机场', '2026-07-08 08:30:00', '2026-07-08 10:45:00', 980, 12, 'sample', now());
insert into flight(flight_no, airline_name, from_city, to_city, from_airport, to_airport, depart_time, arrive_time, price, seats_left, data_source, collected_at)
values ('CA1502', '中国国航', '上海', '北京', '浦东机场', '大兴机场', '2026-07-08 11:20:00', '2026-07-08 13:35:00', 1280, 8, 'sample', now());
insert into flight(flight_no, airline_name, from_city, to_city, from_airport, to_airport, depart_time, arrive_time, price, seats_left, data_source, collected_at)
values ('HU7605', '海南航空', '上海', '北京', '虹桥机场', '首都机场', '2026-07-08 15:00:00', '2026-07-08 17:15:00', 890, 5, 'sample', now());
insert into flight(flight_no, airline_name, from_city, to_city, from_airport, to_airport, depart_time, arrive_time, price, seats_left, data_source, collected_at)
values ('MF8120', '厦门航空', '上海', '北京', '浦东机场', '大兴机场', '2026-07-08 19:40:00', '2026-07-08 21:55:00', 760, 15, 'sample', now());

-- 北京 → 上海 (4班)
insert into flight(flight_no, airline_name, from_city, to_city, from_airport, to_airport, depart_time, arrive_time, price, seats_left, data_source, collected_at)
values ('CA1831', '中国国航', '北京', '上海', '首都机场', '虹桥机场', '2026-07-08 07:00:00', '2026-07-08 09:15:00', 1050, 6, 'sample', now());
insert into flight(flight_no, airline_name, from_city, to_city, from_airport, to_airport, depart_time, arrive_time, price, seats_left, data_source, collected_at)
values ('MU5166', '东方航空', '北京', '上海', '大兴机场', '浦东机场', '2026-07-08 14:30:00', '2026-07-08 16:45:00', 920, 11, 'sample', now());
insert into flight(flight_no, airline_name, from_city, to_city, from_airport, to_airport, depart_time, arrive_time, price, seats_left, data_source, collected_at)
values ('ZH9152', '深圳航空', '北京', '上海', '首都机场', '虹桥机场', '2026-07-08 18:00:00', '2026-07-08 20:10:00', 1150, 9, 'sample', now());
insert into flight(flight_no, airline_name, from_city, to_city, from_airport, to_airport, depart_time, arrive_time, price, seats_left, data_source, collected_at)
values ('3U8960', '四川航空', '北京', '上海', '首都机场', '浦东机场', '2026-07-08 21:00:00', '2026-07-08 23:10:00', 880, 13, 'sample', now());

-- 广州 → 北京 (4班)
insert into flight(flight_no, airline_name, from_city, to_city, from_airport, to_airport, depart_time, arrive_time, price, seats_left, data_source, collected_at)
values ('CZ3105', '南方航空', '广州', '北京', '白云机场', '大兴机场', '2026-07-08 09:10:00', '2026-07-08 12:10:00', 860, 20, 'sample', now());
insert into flight(flight_no, airline_name, from_city, to_city, from_airport, to_airport, depart_time, arrive_time, price, seats_left, data_source, collected_at)
values ('CA1310', '中国国航', '广州', '北京', '白云机场', '首都机场', '2026-07-08 13:30:00', '2026-07-08 16:40:00', 1350, 3, 'sample', now());
insert into flight(flight_no, airline_name, from_city, to_city, from_airport, to_airport, depart_time, arrive_time, price, seats_left, data_source, collected_at)
values ('HU7803', '海南航空', '广州', '北京', '白云机场', '大兴机场', '2026-07-08 20:00:00', '2026-07-08 23:10:00', 650, 25, 'sample', now());
insert into flight(flight_no, airline_name, from_city, to_city, from_airport, to_airport, depart_time, arrive_time, price, seats_left, data_source, collected_at)
values ('ZH9450', '深圳航空', '广州', '北京', '白云机场', '首都机场', '2026-07-08 06:30:00', '2026-07-08 09:40:00', 780, 18, 'sample', now());

-- 深圳 → 上海 (4班)
insert into flight(flight_no, airline_name, from_city, to_city, from_airport, to_airport, depart_time, arrive_time, price, seats_left, data_source, collected_at)
values ('ZH9103', '深圳航空', '深圳', '上海', '宝安机场', '虹桥机场', '2026-07-09 14:20:00', '2026-07-09 16:35:00', 720, 16, 'sample', now());
insert into flight(flight_no, airline_name, from_city, to_city, from_airport, to_airport, depart_time, arrive_time, price, seats_left, data_source, collected_at)
values ('CZ6752', '南方航空', '深圳', '上海', '宝安机场', '浦东机场', '2026-07-09 08:30:00', '2026-07-09 10:50:00', 680, 14, 'sample', now());
insert into flight(flight_no, airline_name, from_city, to_city, from_airport, to_airport, depart_time, arrive_time, price, seats_left, data_source, collected_at)
values ('MU5342', '东方航空', '深圳', '上海', '宝安机场', '虹桥机场', '2026-07-09 19:10:00', '2026-07-09 21:25:00', 810, 7, 'sample', now());
insert into flight(flight_no, airline_name, from_city, to_city, from_airport, to_airport, depart_time, arrive_time, price, seats_left, data_source, collected_at)
values ('HU7721', '海南航空', '深圳', '上海', '宝安机场', '浦东机场', '2026-07-09 12:00:00', '2026-07-09 14:15:00', 950, 8, 'sample', now());

-- 成都 → 北京 (4班)
insert into flight(flight_no, airline_name, from_city, to_city, from_airport, to_airport, depart_time, arrive_time, price, seats_left, data_source, collected_at)
values ('3U8881', '四川航空', '成都', '北京', '双流机场', '首都机场', '2026-07-08 10:00:00', '2026-07-08 12:40:00', 1100, 10, 'sample', now());
insert into flight(flight_no, airline_name, from_city, to_city, from_airport, to_airport, depart_time, arrive_time, price, seats_left, data_source, collected_at)
values ('CA4185', '中国国航', '成都', '北京', '天府机场', '大兴机场', '2026-07-08 16:20:00', '2026-07-08 19:00:00', 1450, 4, 'sample', now());
insert into flight(flight_no, airline_name, from_city, to_city, from_airport, to_airport, depart_time, arrive_time, price, seats_left, data_source, collected_at)
values ('CZ8857', '南方航空', '成都', '北京', '双流机场', '大兴机场', '2026-07-08 07:30:00', '2026-07-08 10:10:00', 1150, 12, 'sample', now());
insert into flight(flight_no, airline_name, from_city, to_city, from_airport, to_airport, depart_time, arrive_time, price, seats_left, data_source, collected_at)
values ('MU5843', '东方航空', '成都', '北京', '天府机场', '首都机场', '2026-07-08 22:00:00', '2026-07-09 00:40:00', 960, 22, 'sample', now());

-- ----- 价格历史快照（每条航班 3 天模拟变化，通过子查询动态匹配 flight_id） -----
-- MU5101: 下降 1050→1020→980
insert into flight_price_snapshot(flight_id, flight_no, from_city, to_city, depart_time, price, seats_left, data_source, observed_at)
select f.id, 'MU5101', '上海', '北京', '2026-07-08 08:30:00', v.price, v.seats, 'sample', v.dt
from flight f, (values row(1050,15,'2026-07-01 09:00:00'),row(1020,14,'2026-07-02 09:00:00'),row(980,12,'2026-07-03 09:00:00')) v(price,seats,dt)
where f.flight_no='MU5101' and f.depart_time='2026-07-08 08:30:00';
-- CA1502: 上升 1180→1250→1280
insert into flight_price_snapshot(flight_id, flight_no, from_city, to_city, depart_time, price, seats_left, data_source, observed_at)
select f.id, 'CA1502', '上海', '北京', '2026-07-08 11:20:00', v.price, v.seats, 'sample', v.dt
from flight f, (values row(1180,12,'2026-07-01 09:00:00'),row(1250,10,'2026-07-02 09:00:00'),row(1280,8,'2026-07-03 09:00:00')) v(price,seats,dt)
where f.flight_no='CA1502' and f.depart_time='2026-07-08 11:20:00';
-- HU7605: 平稳 890→880→890
insert into flight_price_snapshot(flight_id, flight_no, from_city, to_city, depart_time, price, seats_left, data_source, observed_at)
select f.id, 'HU7605', '上海', '北京', '2026-07-08 15:00:00', v.price, v.seats, 'sample', v.dt
from flight f, (values row(890,8,'2026-07-01 09:00:00'),row(880,7,'2026-07-02 09:00:00'),row(890,5,'2026-07-03 09:00:00')) v(price,seats,dt)
where f.flight_no='HU7605' and f.depart_time='2026-07-08 15:00:00';
-- MF8120: 下降 820→780→760
insert into flight_price_snapshot(flight_id, flight_no, from_city, to_city, depart_time, price, seats_left, data_source, observed_at)
select f.id, 'MF8120', '上海', '北京', '2026-07-08 19:40:00', v.price, v.seats, 'sample', v.dt
from flight f, (values row(820,18,'2026-07-01 09:00:00'),row(780,16,'2026-07-02 09:00:00'),row(760,15,'2026-07-03 09:00:00')) v(price,seats,dt)
where f.flight_no='MF8120' and f.depart_time='2026-07-08 19:40:00';
-- CA1831: 上升 980→1020→1050
insert into flight_price_snapshot(flight_id, flight_no, from_city, to_city, depart_time, price, seats_left, data_source, observed_at)
select f.id, 'CA1831', '北京', '上海', '2026-07-08 07:00:00', v.price, v.seats, 'sample', v.dt
from flight f, (values row(980,10,'2026-07-01 09:00:00'),row(1020,8,'2026-07-02 09:00:00'),row(1050,6,'2026-07-03 09:00:00')) v(price,seats,dt)
where f.flight_no='CA1831' and f.depart_time='2026-07-08 07:00:00';
-- MU5166: 下降 960→940→920
insert into flight_price_snapshot(flight_id, flight_no, from_city, to_city, depart_time, price, seats_left, data_source, observed_at)
select f.id, 'MU5166', '北京', '上海', '2026-07-08 14:30:00', v.price, v.seats, 'sample', v.dt
from flight f, (values row(960,15,'2026-07-01 09:00:00'),row(940,13,'2026-07-02 09:00:00'),row(920,11,'2026-07-03 09:00:00')) v(price,seats,dt)
where f.flight_no='MU5166' and f.depart_time='2026-07-08 14:30:00';
-- ZH9152: 平稳 1150→1140→1150
insert into flight_price_snapshot(flight_id, flight_no, from_city, to_city, depart_time, price, seats_left, data_source, observed_at)
select f.id, 'ZH9152', '北京', '上海', '2026-07-08 18:00:00', v.price, v.seats, 'sample', v.dt
from flight f, (values row(1150,12,'2026-07-01 09:00:00'),row(1140,10,'2026-07-02 09:00:00'),row(1150,9,'2026-07-03 09:00:00')) v(price,seats,dt)
where f.flight_no='ZH9152' and f.depart_time='2026-07-08 18:00:00';
-- 3U8960: 下降 920→900→880
insert into flight_price_snapshot(flight_id, flight_no, from_city, to_city, depart_time, price, seats_left, data_source, observed_at)
select f.id, '3U8960', '北京', '上海', '2026-07-08 21:00:00', v.price, v.seats, 'sample', v.dt
from flight f, (values row(920,16,'2026-07-01 09:00:00'),row(900,14,'2026-07-02 09:00:00'),row(880,13,'2026-07-03 09:00:00')) v(price,seats,dt)
where f.flight_no='3U8960' and f.depart_time='2026-07-08 21:00:00';
-- CZ3105: 下降 920→890→860
insert into flight_price_snapshot(flight_id, flight_no, from_city, to_city, depart_time, price, seats_left, data_source, observed_at)
select f.id, 'CZ3105', '广州', '北京', '2026-07-08 09:10:00', v.price, v.seats, 'sample', v.dt
from flight f, (values row(920,25,'2026-07-01 09:00:00'),row(890,22,'2026-07-02 09:00:00'),row(860,20,'2026-07-03 09:00:00')) v(price,seats,dt)
where f.flight_no='CZ3105' and f.depart_time='2026-07-08 09:10:00';
-- CA1310: 上升 1250→1300→1350
insert into flight_price_snapshot(flight_id, flight_no, from_city, to_city, depart_time, price, seats_left, data_source, observed_at)
select f.id, 'CA1310', '广州', '北京', '2026-07-08 13:30:00', v.price, v.seats, 'sample', v.dt
from flight f, (values row(1250,8,'2026-07-01 09:00:00'),row(1300,5,'2026-07-02 09:00:00'),row(1350,3,'2026-07-03 09:00:00')) v(price,seats,dt)
where f.flight_no='CA1310' and f.depart_time='2026-07-08 13:30:00';
-- HU7803: 下降 700→680→650
insert into flight_price_snapshot(flight_id, flight_no, from_city, to_city, depart_time, price, seats_left, data_source, observed_at)
select f.id, 'HU7803', '广州', '北京', '2026-07-08 20:00:00', v.price, v.seats, 'sample', v.dt
from flight f, (values row(700,27,'2026-07-01 09:00:00'),row(680,26,'2026-07-02 09:00:00'),row(650,25,'2026-07-03 09:00:00')) v(price,seats,dt)
where f.flight_no='HU7803' and f.depart_time='2026-07-08 20:00:00';
-- ZH9450: 平稳 790→780→780
insert into flight_price_snapshot(flight_id, flight_no, from_city, to_city, depart_time, price, seats_left, data_source, observed_at)
select f.id, 'ZH9450', '广州', '北京', '2026-07-08 06:30:00', v.price, v.seats, 'sample', v.dt
from flight f, (values row(790,20,'2026-07-01 09:00:00'),row(780,19,'2026-07-02 09:00:00'),row(780,18,'2026-07-03 09:00:00')) v(price,seats,dt)
where f.flight_no='ZH9450' and f.depart_time='2026-07-08 06:30:00';
-- ZH9103: 下降 760→740→720
insert into flight_price_snapshot(flight_id, flight_no, from_city, to_city, depart_time, price, seats_left, data_source, observed_at)
select f.id, 'ZH9103', '深圳', '上海', '2026-07-09 14:20:00', v.price, v.seats, 'sample', v.dt
from flight f, (values row(760,18,'2026-07-01 09:00:00'),row(740,17,'2026-07-02 09:00:00'),row(720,16,'2026-07-03 09:00:00')) v(price,seats,dt)
where f.flight_no='ZH9103' and f.depart_time='2026-07-09 14:20:00';
-- CZ6752: 平稳 680→680→680
insert into flight_price_snapshot(flight_id, flight_no, from_city, to_city, depart_time, price, seats_left, data_source, observed_at)
select f.id, 'CZ6752', '深圳', '上海', '2026-07-09 08:30:00', v.price, v.seats, 'sample', v.dt
from flight f, (values row(680,16,'2026-07-01 09:00:00'),row(680,15,'2026-07-02 09:00:00'),row(680,14,'2026-07-03 09:00:00')) v(price,seats,dt)
where f.flight_no='CZ6752' and f.depart_time='2026-07-09 08:30:00';
-- MU5342: 上升 770→790→810
insert into flight_price_snapshot(flight_id, flight_no, from_city, to_city, depart_time, price, seats_left, data_source, observed_at)
select f.id, 'MU5342', '深圳', '上海', '2026-07-09 19:10:00', v.price, v.seats, 'sample', v.dt
from flight f, (values row(770,10,'2026-07-01 09:00:00'),row(790,8,'2026-07-02 09:00:00'),row(810,7,'2026-07-03 09:00:00')) v(price,seats,dt)
where f.flight_no='MU5342' and f.depart_time='2026-07-09 19:10:00';
-- HU7721: 下降 990→970→950
insert into flight_price_snapshot(flight_id, flight_no, from_city, to_city, depart_time, price, seats_left, data_source, observed_at)
select f.id, 'HU7721', '深圳', '上海', '2026-07-09 12:00:00', v.price, v.seats, 'sample', v.dt
from flight f, (values row(990,11,'2026-07-01 09:00:00'),row(970,9,'2026-07-02 09:00:00'),row(950,8,'2026-07-03 09:00:00')) v(price,seats,dt)
where f.flight_no='HU7721' and f.depart_time='2026-07-09 12:00:00';
-- 3U8881: 平稳 1120→1110→1100
insert into flight_price_snapshot(flight_id, flight_no, from_city, to_city, depart_time, price, seats_left, data_source, observed_at)
select f.id, '3U8881', '成都', '北京', '2026-07-08 10:00:00', v.price, v.seats, 'sample', v.dt
from flight f, (values row(1120,14,'2026-07-01 09:00:00'),row(1110,12,'2026-07-02 09:00:00'),row(1100,10,'2026-07-03 09:00:00')) v(price,seats,dt)
where f.flight_no='3U8881' and f.depart_time='2026-07-08 10:00:00';
-- CA4185: 上升 1350→1400→1450
insert into flight_price_snapshot(flight_id, flight_no, from_city, to_city, depart_time, price, seats_left, data_source, observed_at)
select f.id, 'CA4185', '成都', '北京', '2026-07-08 16:20:00', v.price, v.seats, 'sample', v.dt
from flight f, (values row(1350,8,'2026-07-01 09:00:00'),row(1400,6,'2026-07-02 09:00:00'),row(1450,4,'2026-07-03 09:00:00')) v(price,seats,dt)
where f.flight_no='CA4185' and f.depart_time='2026-07-08 16:20:00';
-- CZ8857: 下降 1200→1180→1150
insert into flight_price_snapshot(flight_id, flight_no, from_city, to_city, depart_time, price, seats_left, data_source, observed_at)
select f.id, 'CZ8857', '成都', '北京', '2026-07-08 07:30:00', v.price, v.seats, 'sample', v.dt
from flight f, (values row(1200,15,'2026-07-01 09:00:00'),row(1180,14,'2026-07-02 09:00:00'),row(1150,12,'2026-07-03 09:00:00')) v(price,seats,dt)
where f.flight_no='CZ8857' and f.depart_time='2026-07-08 07:30:00';
-- MU5843: 下降 1000→980→960
insert into flight_price_snapshot(flight_id, flight_no, from_city, to_city, depart_time, price, seats_left, data_source, observed_at)
select f.id, 'MU5843', '成都', '北京', '2026-07-08 22:00:00', v.price, v.seats, 'sample', v.dt
from flight f, (values row(1000,25,'2026-07-01 09:00:00'),row(980,23,'2026-07-02 09:00:00'),row(960,22,'2026-07-03 09:00:00')) v(price,seats,dt)
where f.flight_no='MU5843' and f.depart_time='2026-07-08 22:00:00';

-- 采集任务记录（模拟一次成功的样例采集）
insert into crawl_job(status, started_at, finished_at, success_count, failed_count, error_message, source, request_params)
values ('SUCCESS', now(), now(), 20, 0, null, 'sample', 'source=sample, fromCity=上海, toCity=北京, date=2026-07-08');
