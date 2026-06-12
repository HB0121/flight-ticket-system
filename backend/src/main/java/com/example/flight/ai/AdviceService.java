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

@Service
public class AdviceService {

    private static final Logger log = LoggerFactory.getLogger(AdviceService.class);
    private final FlightSearchPort flightSearchPort;
    private final AiTextClient aiTextClient;

    @Autowired
    public AdviceService(FlightSearchPort flightSearchPort, AiTextClient aiTextClient) {
        this.flightSearchPort = flightSearchPort;
        this.aiTextClient = aiTextClient;
    }

    public AdviceService(FlightSearchPort flightSearchPort) {
        this(flightSearchPort, (systemPrompt, userPrompt) -> Optional.empty());
    }

    public AdviceResponse generate(AdviceRequest request) {
        String message = request == null || request.message() == null ? "" : request.message();
        ParsedTravelIntent intent = TravelIntentParser.parse(message);
        var criteria = new FlightSearchCriteria(intent.fromCity(), intent.toCity(), intent.date());
        List<Flight> candidates = flightSearchPort.search(criteria);

        Optional<Flight> recommended = candidates.stream()
                .filter(flight -> intent.budget() == null || flight.price().compareTo(intent.budget()) <= 0)
                .min(Comparator.comparing(Flight::price).thenComparing(Flight::departTime));

        if (recommended.isEmpty()) {
            String route = routeText(intent);
            log.info("出行建议: 未找到匹配航班, route={}", route);
            return new AdviceResponse("暂未找到" + route + "符合预算的航班，可以放宽预算或更换日期后再试。", null, candidates);
        }

        Flight flight = recommended.get();
        log.info("出行建议: 推荐航班 {}, 价格 {}", flight.flightNo(), flight.price());
        Optional<String> aiSummary = aiTextClient.generate(
                "你是机票出行建议助手。基于候选航班给出简洁、可执行的中文建议。",
                buildAdvicePrompt(message, intent, flight, candidates)
        );
        if (aiSummary.isPresent()) {
            log.debug("出行建议: 使用AI生成摘要");
            return new AdviceResponse(aiSummary.get(), flight, candidates);
        }

        log.debug("出行建议: 使用本地规则生成摘要");

        String budgetText = intent.budget() == null ? "当前最低价" : "预算内";
        String summary = "推荐 " + flight.flightNo() + "，" + flight.fromCity() + "到" + flight.toCity()
                + "，" + flight.departTime().toLocalDate() + " " + flight.departTime().toLocalTime()
                + " 起飞，价格 " + flight.price().stripTrailingZeros().toPlainString()
                + " 元，属于" + budgetText + "选择。";
        return new AdviceResponse(summary, flight, candidates);
    }

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
        Optional<String> aiSummary = aiTextClient.generate(
                "你是机票出行建议助手。根据对话历史和当前查询，给出简洁、连贯的中文建议。",
                buildConversationPrompt(message, intent, flight, candidates, history)
        );
        String summary = aiSummary.orElseGet(() -> {
            String budgetText = intent.budget() == null ? "当前最低价" : "预算内";
            return "推荐 " + flight.flightNo() + "，" + flight.fromCity() + "到" + flight.toCity()
                    + "，" + flight.departTime().toLocalDate() + " " + flight.departTime().toLocalTime()
                    + " 起飞，价格 " + flight.price().stripTrailingZeros().toPlainString()
                    + " 元，属于" + budgetText + "选择。";
        });
        return new AdviceResponse(summary, flight, candidates);
    }

    private String routeText(ParsedTravelIntent intent) {
        if (intent.fromCity() != null && intent.toCity() != null) {
            return intent.fromCity() + "到" + intent.toCity();
        }
        return "";
    }

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

    private String buildConversationPrompt(String message, ParsedTravelIntent intent,
                                            Flight recommended, List<Flight> candidates,
                                            List<ConversationMessage> history) {
        String budget = intent.budget() == null ? "未提供" : intent.budget().toPlainString() + "元";
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
