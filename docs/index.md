# 仿 12306 机票抓取与自动更新系统

本项目是综合课程设计 III 的第 7 题：**基于网络爬虫的机票抓取与自动更新系统设计与实现**。

## 第一版目标

第一版先完成稳定可演示的最小闭环：

1. Scrapy 解析本地样例 HTML，模拟采集机票数据。
2. MySQL 保存航班数据和采集任务。
3. SpringBoot 提供航班查询、采集触发和 AI 出行建议接口。
4. Vue 3 PC 端展示采集状态、航班列表、价格图表和 AI 建议。

## 项目文档

- [第一版框架](https://github.com/HB0121/flight-ticket-system/blob/main/%E7%AC%AC%E4%B8%80%E7%89%88%E6%A1%86%E6%9E%B6.md)
- [数据端整改记录（2026-06-12）](./data-remediation-2026-06-12.md)
- [GitHub 仓库](https://github.com/HB0121/flight-ticket-system)

## 演示路径

1. 启动 MySQL。
2. 启动 SpringBoot 后端。
3. 启动 Vue 前端。
4. 点击采集按钮写入样例航班。
5. 查询上海到北京航班。
6. 输入“2026-06-19 上海到北京，预算1200元”，查看 AI 推荐。
