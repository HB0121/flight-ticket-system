package com.example.flight.ai;

import com.example.flight.flight.Flight;
import com.example.flight.flight.FlightPriceSnapshot;
import com.example.flight.flight.FlightSearchCriteria;
import com.example.flight.flight.FlightSearchPort;
import com.example.flight.flight.PriceHistoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 购票时机分析服务 —— 分析航班价格趋势并提供最佳购票时机建议。
 *
 * 核心分析流程（多数据源融合）：
 * 1. 意图解析 —— 通过 {@link TravelIntentParser} 提取城市、日期、预算
 * 2. 航班查询 —— 搜索候选航班，选出推荐航班
 * 3. 价格趋势分析 —— 对比历史快照的首末价格，判断上涨/下跌/平稳
 * 4. RAG 检索增强 —— 从 {@link PriceContextRepository} 检索该航线的历史价格规律
 *    a. 优先按出发城市+目的城市精确检索
 *    b. 若无结果，降级为关键词全文搜索
 * 5. 节假日接近度计算 —— 通过 {@link HolidayProximityCalculator} 判断是否临近假期
 * 6. 摘要生成：
 *    a. 首选：调用 AI 客户端生成智能分析报告
 *    b. 降级：使用本地规则拼接分析报告（趋势方向 + 风险等级 + 节假日提示 + RAG 上下文 + 购票窗口）
 *
 * 这种 "趋势分析 + RAG检索 + 节假日提示 + AI摘要" 的四层融合模式提供了全面的购票决策支持。
 */
@Service
public class TimingService {

    private static final Logger log = LoggerFactory.getLogger(TimingService.class);
    /** 航班搜索端口 */
    private final FlightSearchPort flightSearchPort;
    /** 价格历史端口 —— 用于获取航班的历史价格快照 */
    private final PriceHistoryPort priceHistoryPort;
    /** AI 文本生成客户端 */
    private final AiTextClient aiTextClient;
    /** RAG 价格上下文仓储 —— 存储和检索航线的历史价格规律知识 */
    private final PriceContextRepository priceContextRepository;

    /**
     * 构造函数 —— 注入四个依赖。
     *
     * @param flightSearchPort      航班搜索接口
     * @param priceHistoryPort      价格历史接口
     * @param aiTextClient          AI 客户端
     * @param priceContextRepository RAG 上下文仓储
     */
    public TimingService(FlightSearchPort flightSearchPort,
                         PriceHistoryPort priceHistoryPort,
                         AiTextClient aiTextClient,
                         PriceContextRepository priceContextRepository) {
        this.flightSearchPort = flightSearchPort;
        this.priceHistoryPort = priceHistoryPort;
        this.aiTextClient = aiTextClient;
        this.priceContextRepository = priceContextRepository;
    }

    /**
     * 分析购票时机 —— 服务的唯一公开入口。
     *
     * 完整分析流程：
     * 1. 解析用户消息 → 出行意图
     * 2. 搜索航班 → 选推荐航班
     * 3. 加载价格历史 → 计算趋势方向（上涨/下跌/平稳）和风险等级
     * 4. RAG 检索 → 按航线查找历史价格规律，无结果时降级为关键词搜索
     * 5. 节假日接近度 → 计算距离最近假期的天数及影响程度
     * 6. 生成摘要 → AI 优先，本地降级
     *
     * @param request 购票时机分析请求
     * @return TimingResponse 包含分析摘要、风险等级、购票窗口、推荐航班和价格历史
     */
    public TimingResponse analyze(TimingRequest request) {
        String message = request == null || request.message() == null ? "" : request.message();
        // 第1步：解析用户意图
        ParsedTravelIntent intent = TravelIntentParser.parse(message);
        // 第2步：搜索航班并选出推荐航班
        List<Flight> candidates = flightSearchPort.search(new FlightSearchCriteria(intent.fromCity(), intent.toCity(), intent.date()));
        Optional<Flight> recommended = candidates.stream()
                .filter(flight -> intent.budget() == null || flight.price().compareTo(intent.budget()) <= 0)
                .min(Comparator.comparing(Flight::price).thenComparing(Flight::departTime));

        if (recommended.isEmpty()) {
            return new TimingResponse("暂未找到可分析的航班，请先采集对应航线数据。", "UNKNOWN", "暂无建议", null, List.of());
        }

        Flight flight = recommended.get();
        // 第3步：加载价格历史并分析趋势
        List<FlightPriceSnapshot> history = priceHistoryPort.findPriceHistory(flight.id());
        log.info("购票时机分析: 航班 {}, 历史快照数 {}, 当前价 {}", flight.flightNo(), history.size(), flight.price());
        Trend trend = analyzeTrend(history);
        log.debug("购票时机分析: 趋势 direction={}, riskLevel={}", trend.direction(), trend.riskLevel());

        // 第4步：RAG 检索 —— 优先按城市对精确匹配，无结果则降级为关键词全文检索
        List<String> ragContexts = priceContextRepository.searchContext(flight.fromCity(), flight.toCity());
        final List<String> finalRagContexts = ragContexts.isEmpty()
                ? priceContextRepository.searchByKeyword(flight.fromCity() + " " + flight.toCity())
                : ragContexts;
        log.debug("购票时机分析: RAG检索到{}条上下文", finalRagContexts.size());

        // 第5步：节假日接近度分析
        final String holidayInfo = intent.date() != null
                ? HolidayProximityCalculator.holidayAdvice(intent.date())
                : "";

        // 第6步：尝试 AI 生成摘要，失败则使用本地规则
        Optional<String> aiSummary = aiTextClient.generate(
                "你是机票购票时机分析助手。根据航班价格历史、历史规律和节假日信息，生成简洁中文购票时机报告。",
                buildTimingPrompt(message, flight, history, trend, finalRagContexts, holidayInfo)
        );
        String summary = aiSummary.orElseGet(() -> buildLocalSummary(flight, trend, history, finalRagContexts, holidayInfo));
        log.info("购票时机分析完成: riskLevel={}", trend.riskLevel());
        return new TimingResponse(summary, trend.riskLevel(), trend.buyWindow(), flight, history);
    }

    /**
     * 分析价格趋势 —— 核心算法。
     *
     * 逻辑：
     * - 如果历史快照不足2条 → 样本不足，风险等级 MEDIUM
     * - 如果最新价格 < 最早价格   → 价格下降，风险 LOW，建议近期关注
     * - 如果最新价格 > 最早价格   → 价格上涨，风险 HIGH，建议尽快购买
     * - 如果价格持平               → 风险 MEDIUM，建议继续观察
     *
     * 这是一个简单的首末价格对比算法。更高级的实现可以加入
     * 移动平均线、波动率计算、季节性分解等。
     *
     * @param history 按时间正序排列的价格快照列表（最早在前）
     * @return Trend 记录：风险等级 + 购票窗口建议 + 趋势方向
     */
    private Trend analyzeTrend(List<FlightPriceSnapshot> history) {
        if (history.size() < 2) {
            return new Trend("MEDIUM", "建议继续采集 2-3 次后再判断", "样本不足");
        }
        int first = history.get(0).price();
        int last = history.get(history.size() - 1).price();
        if (last < first) {
            return new Trend("LOW", "建议 3 天内重点关注，低于当前价可购买", "下降");
        }
        if (last > first) {
            return new Trend("HIGH", "价格已有上涨迹象，刚需出行建议尽快购买", "上涨");
        }
        return new Trend("MEDIUM", "价格暂时平稳，可继续观察 1-2 天", "平稳");
    }

    /**
     * 本地规则生成分析摘要 —— AI 不可用时的降级方案。
     *
     * 组装逻辑：
     * 1. 航班基本信息（航班号、航线、当前价）
     * 2. 趋势判断（样本不足 / 趋势方向 + 风险等级）
     * 3. 节假日提示（如有）
     * 4. RAG 历史规律（取第一条，截断至100字）
     * 5. 购票窗口建议
     *
     * @param flight      推荐航班
     * @param trend       趋势分析结果
     * @param history     价格历史
     * @param ragContexts RAG 检索到的历史规律文本列表
     * @param holidayInfo 节假日提示文本
     * @return 拼接好的分析摘要字符串
     */
    private String buildLocalSummary(Flight flight, Trend trend, List<FlightPriceSnapshot> history,
                                      List<String> ragContexts, String holidayInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append("本地分析：").append(flight.flightNo())
                .append(" ").append(flight.fromCity()).append("→").append(flight.toCity())
                .append("，当前价 ").append(flight.price().stripTrailingZeros().toPlainString()).append(" 元。");

        if (history.size() < 2) {
            sb.append("价格样本不足，建议再采集2-3次。");
        } else {
            sb.append("价格正在").append(trend.direction()).append("，风险等级").append(trend.riskLevel()).append("。");
        }

        // 附加节假日提示
        if (!holidayInfo.isBlank()) {
            sb.append(" ").append(holidayInfo);
        }

        // 附加 RAG 历史规律（截断过长文本）
        if (!ragContexts.isEmpty()) {
            sb.append(" 参考历史规律：").append(ragContexts.get(0).length() > 100
                    ? ragContexts.get(0).substring(0, 100) + "..."
                    : ragContexts.get(0));
        }

        sb.append(" ").append(trend.buyWindow());
        return sb.toString();
    }

    /**
     * 构建 AI 提示词 —— 将趋势、RAG、节假日信息拼接为结构化的 prompt。
     *
     * Prompt 结构：
     * - 用户需求
     * - 推荐航班详情
     * - 价格快照（原始数据供 AI 分析）
     * - 本地趋势判断（作为 AI 分析的参考基线）
     * - RAG 历史规律（最多3条）
     * - 节假日提示
     * - 输出要求（150字以内）
     *
     * @param message     用户原始消息
     * @param flight      推荐航班
     * @param history     价格历史
     * @param trend       本地趋势分析
     * @param ragContexts RAG 历史规律
     * @param holidayInfo 节假日信息
     * @return 拼接好的 prompt 字符串
     */
    private String buildTimingPrompt(String message, Flight flight, List<FlightPriceSnapshot> history,
                                      Trend trend, List<String> ragContexts, String holidayInfo) {
        String ragSection = ragContexts.isEmpty() ? "暂无历史规律数据"
                : "历史规律：\n" + String.join("\n", ragContexts.stream().limit(3).toList());
        String holidaySection = holidayInfo.isBlank() ? "" : "节假日提示：" + holidayInfo;

        return String.join("\n",
                "用户需求：" + message,
                "推荐航班：" + flight.flightNo() + "，" + flight.fromCity() + "到" + flight.toCity()
                        + "，起飞 " + flight.departTime() + "，当前价 "
                        + flight.price().stripTrailingZeros().toPlainString() + " 元",
                "价格快照：" + history,
                "本地趋势判断：" + trend.direction() + "，风险等级 " + trend.riskLevel()
                        + "，窗口建议：" + trend.buyWindow(),
                ragSection,
                holidaySection,
                "请结合以上信息，输出 150 字以内购票时机报告（含最佳购票时间段和涨价风险提示）。"
        );
    }

    /**
     * 趋势分析结果 —— 内部数据载体。
     *
     * @param riskLevel 风险等级：LOW（可等待）/ MEDIUM（需观察）/ HIGH（尽快购买）
     * @param buyWindow 购票时间窗口建议文本
     * @param direction 趋势方向：上涨 / 下跌 / 平稳 / 样本不足
     */
    private record Trend(String riskLevel, String buyWindow, String direction) {
    }
}
