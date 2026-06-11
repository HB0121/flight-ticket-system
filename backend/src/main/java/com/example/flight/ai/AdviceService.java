package com.example.flight.ai;

import com.example.flight.flight.Flight;
import com.example.flight.flight.FlightSearchCriteria;
import com.example.flight.flight.FlightSearchPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class AdviceService {
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
            return new AdviceResponse("暂未找到" + route + "符合预算的航班，可以放宽预算或更换日期后再试。", null, candidates);
        }

        Flight flight = recommended.get();
        Optional<String> aiSummary = aiTextClient.generate(
                "你是机票出行建议助手。基于候选航班给出简洁、可执行的中文建议。",
                buildAdvicePrompt(message, intent, flight, candidates)
        );
        if (aiSummary.isPresent()) {
            return new AdviceResponse(aiSummary.get(), flight, candidates);
        }

        String budgetText = intent.budget() == null ? "当前最低价" : "预算内";
        String summary = "推荐 " + flight.flightNo() + "，" + flight.fromCity() + "到" + flight.toCity()
                + "，" + flight.departTime().toLocalDate() + " " + flight.departTime().toLocalTime()
                + " 起飞，价格 " + flight.price().stripTrailingZeros().toPlainString()
                + " 元，属于" + budgetText + "选择。";
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
        return """
                用户需求：%s
                解析结果：%s -> %s，日期 %s，预算 %s
                推荐航班：%s，%s -> %s，起飞 %s，到达 %s，价格 %s 元，余票 %s
                候选航班数量：%s
                请用 80 字以内给出推荐理由和购票建议。
                """.formatted(
                message,
                intent.fromCity(), intent.toCity(), intent.date(), budget,
                recommended.flightNo(), recommended.fromAirport(), recommended.toAirport(),
                recommended.departTime(), recommended.arriveTime(),
                recommended.price().stripTrailingZeros().toPlainString(), recommended.seatsLeft(),
                candidates.size()
        );
    }
}
