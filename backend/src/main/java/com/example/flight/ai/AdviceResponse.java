package com.example.flight.ai;

import com.example.flight.flight.Flight;

import java.util.List;

/**
 * 出行建议响应体 —— POST /api/ai/advice 的返回 DTO。
 *
 * @param summary           AI 生成的建议摘要文本（或本地规则生成的降级文本），
 *                          向用户展示的最终建议内容
 * @param recommendedFlight 经过价格排序和预算过滤后推荐的最佳航班，
 *                          如果无匹配航班则为 null
 * @param candidates        所有匹配搜索条件的候选航班列表，
 *                          前端可用于展示多选项对比
 */
public record AdviceResponse(
        String summary,
        Flight recommendedFlight,
        List<Flight> candidates
) {
}
