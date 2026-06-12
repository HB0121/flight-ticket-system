package com.example.flight.ai;

import com.example.flight.flight.Flight;
import com.example.flight.flight.FlightSearchCriteria;
import com.example.flight.flight.FlightSearchPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 出行建议服务 —— 系统的核心业务服务。
 *
 * 处理流程（三层降级链）：
 * 1. 解析用户自然语言消息 → {@link TravelIntentParser} 提取城市、日期、预算
 * 2. 查询航班数据 → 通过 {@link FlightSearchPort} 搜索匹配航班
 * 3. 筛选推荐航班 → 按预算过滤，取最低价 + 最早起飞时间
 * 4. 生成建议摘要：
 *    a. 首选：调用 {@link AiTextClient}（DeepSeek AI）生成智能摘要
 *    b. 降级：当 AI 不可用（返回 Optional.empty()）时，使用本地规则拼装摘要文本
 *
 * 这种"AI 优先 + 本地兜底"的模式保证了系统在任何环境下都能提供可用的出行建议。
 */
@Service
public class AdviceService {

    private static final Logger log = LoggerFactory.getLogger(AdviceService.class);
    /** 航班搜索端口 —— 通过接口解耦，便于测试和替换实现 */
    private final FlightSearchPort flightSearchPort;
    /** AI 文本生成客户端 —— 可能是 DeepSeek 或降级 lambda */
    private final AiTextClient aiTextClient;

    /**
     * 主构造函数 —— 注入航班搜索端口和 AI 客户端。
     *
     * @param flightSearchPort 航班数据访问接口
     * @param aiTextClient     AI 文本生成客户端（通常为 DeepSeekTextClient）
     */
    @Autowired
    public AdviceService(FlightSearchPort flightSearchPort, AiTextClient aiTextClient) {
        this.flightSearchPort = flightSearchPort;
        this.aiTextClient = aiTextClient;
    }

    /**
     * 降级构造函数 —— 仅注入航班搜索端口，AI 客户端使用空实现（永远返回 empty）。
     * 当 Spring 容器中没有 AiTextClient Bean 时（如 DeepSeek 依赖未加载），
     * 通过此构造函数创建一个纯本地规则版本的 AdviceService。
     *
     * @param flightSearchPort 航班数据访问接口
     */
    public AdviceService(FlightSearchPort flightSearchPort) {
        this(flightSearchPort, (systemPrompt, userPrompt) -> Optional.empty());
    }

    /**
     * 生成出行建议（无会话上下文）—— 单次请求的出行建议入口。
     *
     * 处理流程：
     * 1. 安全获取用户消息（null → ""）
     * 2. 调用 TravelIntentParser.parse 提取结构化意图
     * 3. 通过 FlightSearchPort 搜索候选航班
     * 4. 在候选航班中按预算过滤，取最低价推荐
     * 5. 尝试 AI 生成摘要，失败则回退到本地规则拼装
     *
     * @param request 出行建议请求（包含用户自然语言消息）
     * @return AdviceResponse 包含摘要、推荐航班和候选列表
     */
    public AdviceResponse generate(AdviceRequest request) {
        String message = request == null || request.message() == null ? "" : request.message();
        // 解析用户意图：城市、日期、预算
        ParsedTravelIntent intent = TravelIntentParser.parse(message);
        var criteria = new FlightSearchCriteria(intent.fromCity(), intent.toCity(), intent.date());
        // 查询所有匹配航班
        List<Flight> candidates = flightSearchPort.search(criteria);

        // 在预算范围内选择推荐航班：价格最低优先，同价取最早起飞时间
        Optional<Flight> recommended = candidates.stream()
                .filter(flight -> intent.budget() == null || flight.price().compareTo(intent.budget()) <= 0)
                .min(Comparator.comparing(Flight::price).thenComparing(Flight::departTime));

        // 如果没有符合条件的航班，返回提示信息
        if (recommended.isEmpty()) {
            String route = routeText(intent);
            log.info("出行建议: 未找到匹配航班, route={}", route);
            return new AdviceResponse("暂未找到" + route + "符合预算的航班，可以放宽预算或更换日期后再试。", null, candidates);
        }

        Flight flight = recommended.get();
        log.info("出行建议: 推荐航班 {}, 价格 {}", flight.flightNo(), flight.price());
        // 尝试调用 AI 生成建议摘要（DeepSeek 可用时）
        Optional<String> aiSummary = aiTextClient.generate(
                "你是机票出行建议助手。基于候选航班给出简洁、可执行的中文建议。",
                buildAdvicePrompt(message, intent, flight, candidates)
        );
        if (aiSummary.isPresent()) {
            log.debug("出行建议: 使用AI生成摘要");
            return new AdviceResponse(aiSummary.get(), flight, candidates);
        }

        // AI 不可用 —— 回退到本地规则引擎生成摘要
        log.debug("出行建议: 使用本地规则生成摘要");

        String budgetText = intent.budget() == null ? "当前最低价" : "预算内";
        String summary = "推荐 " + flight.flightNo() + "，" + flight.fromCity() + "到" + flight.toCity()
                + "，" + flight.departTime().toLocalDate() + " " + flight.departTime().toLocalTime()
                + " 起飞，价格 " + flight.price().stripTrailingZeros().toPlainString()
                + " 元，属于" + budgetText + "选择。";
        return new AdviceResponse(summary, flight, candidates);
    }

    /**
     * 生成出行建议（带会话上下文）—— 多轮对话场景的出行建议入口。
     *
     * 与 generate() 的区别在于：
     * - 构建 prompt 时会包含对话历史，使 AI 的回复更连贯
     * - 返回的摘要会被写回 conversation_message 表（由 Controller 负责）
     *
     * @param sessionId 会话 ID
     * @param message   用户当前消息
     * @param history   该会话的历史消息列表（包含当前消息之前的所有记录）
     * @return AdviceResponse 包含摘要、推荐航班和候选列表
     */
    public AdviceResponse generateInSession(String sessionId, String message,
                                             List<ConversationMessage> history) {
        ParsedTravelIntent intent = TravelIntentParser.parse(message);
        var criteria = new FlightSearchCriteria(intent.fromCity(), intent.toCity(), intent.date());
        List<Flight> candidates = flightSearchPort.search(criteria);

        Optional<Flight> recommended = candidates.stream()
                .filter(flight -> intent.budget() == null || flight.price().compareTo(intent.budget()) <= 0)
                .min(Comparator.comparing(Flight::price).thenComparing(Flight::departTime));

        if (recommended.isEmpty()) {
            String route = routeText(intent);
            return new AdviceResponse("暂未找到" + route + "符合预算的航班，可以放宽预算或更换日期后再试。", null, candidates);
        }

        Flight flight = recommended.get();
        // 构建包含对话历史的 prompt，使 AI 回复更连贯
        Optional<String> aiSummary = aiTextClient.generate(
                "你是机票出行建议助手。根据对话历史和当前查询，给出简洁、连贯的中文建议。",
                buildConversationPrompt(message, intent, flight, candidates, history)
        );
        // AI 不可用时回退到本地规则
        String summary = aiSummary.orElseGet(() -> {
            String budgetText = intent.budget() == null ? "当前最低价" : "预算内";
            return "推荐 " + flight.flightNo() + "，" + flight.fromCity() + "到" + flight.toCity()
                    + "，" + flight.departTime().toLocalDate() + " " + flight.departTime().toLocalTime()
                    + " 起飞，价格 " + flight.price().stripTrailingZeros().toPlainString()
                    + " 元，属于" + budgetText + "选择。";
        });
        return new AdviceResponse(summary, flight, candidates);
    }

    /**
     * 将出行意图格式化为可读的航线文本。
     *
     * @param intent 已解析的出行意图
     * @return 如 "上海到北京" 的字符串；如果城市信息不全则返回空字符串
     */
    private String routeText(ParsedTravelIntent intent) {
        if (intent.fromCity() != null && intent.toCity() != null) {
            return intent.fromCity() + "到" + intent.toCity();
        }
        return "";
    }

    /**
     * 构建 AI 提示词（单次请求版本）—— 包含用户需求、解析结果和推荐航班信息。
     *
     * @param message    用户原始消息
     * @param intent     解析后的出行意图
     * @param recommended 推荐的航班
     * @param candidates  候选航班列表
     * @return 拼接好的 AI 提示词文本
     */
    private String buildAdvicePrompt(String message, ParsedTravelIntent intent, Flight recommended, List<Flight> candidates) {
        String budget = intent.budget() == null ? "未提供" : intent.budget().toPlainString() + "元";
        return String.join("\n",
                "用户需求：" + message,
                "解析结果：" + intent.fromCity() + " -> " + intent.toCity()
                        + "，日期 " + intent.date() + "，预算 " + budget,
                "推荐航班：" + recommended.flightNo() + "，"
                        + recommended.fromAirport() + " -> " + recommended.toAirport()
                        + "，起飞 " + recommended.departTime() + "，到达 " + recommended.arriveTime()
                        + "，价格 " + recommended.price().stripTrailingZeros().toPlainString()
                        + " 元，余票 " + recommended.seatsLeft(),
                "候选航班数量：" + candidates.size(),
                "请用 80 字以内给出推荐理由和购票建议。"
        );
    }

    /**
     * 构建 AI 提示词（多轮对话版本）—— 在 prompt 中加入对话历史，使 AI 回复更连贯。
     *
     * @param message     用户当前消息
     * @param intent      解析后的出行意图
     * @param recommended 推荐的航班
     * @param candidates  候选航班列表
     * @param history     该会话的历史消息列表
     * @return 拼接好的 AI 提示词文本，包含对话历史
     */
    private String buildConversationPrompt(String message, ParsedTravelIntent intent,
                                            Flight recommended, List<Flight> candidates,
                                            List<ConversationMessage> history) {
        String budget = intent.budget() == null ? "未提供" : intent.budget().toPlainString() + "元";
        // 将历史消息序列化为 "role: content" 格式（排除最后一条，即当前消息本身）
        String historyText = history.stream()
                .filter(m -> history.indexOf(m) < history.size() - 1)
                .map(m -> m.role() + ": " + m.content())
                .collect(Collectors.joining("\n"));
        return String.join("\n",
                "对话历史：",
                historyText,
                "",
                "当前用户需求：" + message,
                "解析结果：" + intent.fromCity() + " -> " + intent.toCity()
                        + "，日期 " + intent.date() + "，预算 " + budget,
                "推荐航班：" + recommended.flightNo() + "，"
                        + recommended.fromAirport() + " -> " + recommended.toAirport()
                        + "，起飞 " + recommended.departTime()
                        + "，到达 " + recommended.arriveTime()
                        + "，价格 " + recommended.price().stripTrailingZeros().toPlainString()
                        + " 元，余票 " + recommended.seatsLeft(),
                "候选航班数量：" + candidates.size(),
                "请结合对话历史，用 100 字以内给出连贯的推荐理由和购票建议。"
        );
    }
}
