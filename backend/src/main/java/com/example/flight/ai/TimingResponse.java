package com.example.flight.ai;

import com.example.flight.flight.Flight;
import com.example.flight.flight.FlightPriceSnapshot;

import java.util.List;

/**
 * 购票时机分析响应体 —— POST /api/ai/timing 的返回 DTO。
 *
 * @param summary           分析摘要文本（AI 生成或本地规则生成），
 *                          包含价格趋势、购票建议和风险提示
 * @param riskLevel         风险等级：LOW（低风险，可等待）/ MEDIUM（中等，需观察）/ HIGH（高风险，建议立即购买）
 * @param buyWindow         建议的购票时间窗口描述，如"建议3天内重点关注"
 * @param recommendedFlight 分析的目标航班（推荐航班）
 * @param history           该航班的历史价格快照列表，前端可用于绘制价格趋势图
 */
public record TimingResponse(
        String summary,
        String riskLevel,
        String buyWindow,
        Flight recommendedFlight,
        List<FlightPriceSnapshot> history
) {
}
