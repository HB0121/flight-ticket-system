package com.example.flight.crawl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 爬虫请求参数对象，封装前端触发的爬虫执行配置。
 * 包含数据来源选择、航线参数、搜索限制等。
 *
 * 核心职责：将用户请求转换为 Scrapy 命令行参数列表。
 * "amadeus" 来源会拼接完整的 Spider 参数（from_city, to_city, date 等），
 * "sample" 来源则使用固定命令 "scrapy crawl sample_flights"（无额外参数）。
 *
 * 设计模式：数据传输对象（DTO），负责前端请求格式到爬虫 CLI 参数的转换。
 *
 * @param source     数据来源（"amadeus" 或 "sample"，大小写不敏感）
 * @param fromCity   出发城市中文名
 * @param toCity     到达城市中文名
 * @param date       出发日期
 * @param adults     成人数
 * @param maxResults 最大返回结果数
 */
public record CrawlRequest(
        String source,
        String fromCity,
        String toCity,
        LocalDate date,
        Integer adults,
        Integer maxResults
) {
    /**
     * 将 source 标准化为 "amadeus" 或 "sample"。
     * 大小写不敏感，非法值默认降级为 "sample"。
     *
     * @return 标准化的数据来源标识
     */
    public String normalizedSource() {
        return "amadeus".equalsIgnoreCase(source) ? "amadeus" : "sample";
    }

    /**
     * 将请求参数转换为 Scrapy 命令行参数列表。
     *
     * "sample" 来源：返回固定命令 ["scrapy", "crawl", "sample_flights"]（无动态参数）
     * "amadeus" 来源：返回完整命令，包含 -a 键值对参数：
     *   scrapy crawl amadeus_flights -a from_city=上海 -a to_city=北京 -a date=2026-06-19 ...
     *
     * 所有空值参数均有合理默认值（出发=上海，到达=北京，日期=7天后，成人数=1，最大结果=5）。
     *
     * @return 不可变的命令行参数列表，供 ProcessBuilder 使用
     */
    public List<String> toCrawlerArguments() {
        if (!"amadeus".equals(normalizedSource())) {
            return List.of("scrapy", "crawl", "sample_flights"); // 静态样本模式，无需动态参数
        }
        var args = new ArrayList<String>();
        args.add("scrapy");
        args.add("crawl");
        args.add("amadeus_flights");
        // Scrapy 通过 -a key=value 传递 Spider 参数
        args.add("-a");
        args.add("from_city=" + valueOrDefault(fromCity, "上海"));
        args.add("-a");
        args.add("to_city=" + valueOrDefault(toCity, "北京"));
        args.add("-a");
        args.add("date=" + (date == null ? LocalDate.now().plusDays(7) : date));
        args.add("-a");
        args.add("adults=" + (adults == null || adults < 1 ? 1 : adults));
        args.add("-a");
        args.add("max_results=" + (maxResults == null || maxResults < 1 ? 5 : maxResults));
        return args;
    }

    /**
     * 生成请求参数的人类可读摘要字符串。
     * 用于存入 crawl_job.request_params 字段，方便在数据库和日志中追踪。
     *
     * @return 格式如 "source=amadeus, fromCity=上海, toCity=北京, date=2026-06-19, adults=1, maxResults=5"
     */
    public String toSummary() {
        return "source=" + normalizedSource()
                + ", fromCity=" + valueOrDefault(fromCity, "上海")
                + ", toCity=" + valueOrDefault(toCity, "北京")
                + ", date=" + (date == null ? LocalDate.now().plusDays(7) : date)
                + ", adults=" + (adults == null || adults < 1 ? 1 : adults)
                + ", maxResults=" + (maxResults == null || maxResults < 1 ? 5 : maxResults);
    }

    /**
     * 安全取值工具方法：若 value 为 null 或空白字符串，返回 fallback，否则返回 trim 后的值。
     * 确保爬虫命令不包含空字符串参数。
     */
    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
