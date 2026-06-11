package com.example.flight.ai;

import com.example.flight.flight.Flight;
import com.example.flight.flight.FlightPriceSnapshot;
import com.example.flight.flight.FlightSearchCriteria;
import com.example.flight.flight.FlightSearchPort;
import com.example.flight.flight.PriceHistoryPort;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class TimingService {
    private final FlightSearchPort flightSearchPort;
    private final PriceHistoryPort priceHistoryPort;
    private final AiTextClient aiTextClient;

    public TimingService(FlightSearchPort flightSearchPort,
                         PriceHistoryPort priceHistoryPort,
                         AiTextClient aiTextClient) {
        this.flightSearchPort = flightSearchPort;
        this.priceHistoryPort = priceHistoryPort;
        this.aiTextClient = aiTextClient;
    }

    public TimingResponse analyze(TimingRequest request) {
        String message = request == null || request.message() == null ? "" : request.message();
        ParsedTravelIntent intent = TravelIntentParser.parse(message);
        List<Flight> candidates = flightSearchPort.search(new FlightSearchCriteria(intent.fromCity(), intent.toCity(), intent.date()));
        Optional<Flight> recommended = candidates.stream()
                .filter(flight -> intent.budget() == null || flight.price().compareTo(intent.budget()) <= 0)
                .min(Comparator.comparing(Flight::price).thenComparing(Flight::departTime));

        if (recommended.isEmpty()) {
            return new TimingResponse("暂未找到可分析的航班，请先采集对应航线数据。", "UNKNOWN", "暂无建议", null, List.of());
        }

        Flight flight = recommended.get();
        List<FlightPriceSnapshot> history = priceHistoryPort.findPriceHistory(flight.id());
        Trend trend = analyzeTrend(history);
        Optional<String> aiSummary = aiTextClient.generate(
                "你是机票购票时机分析助手。根据航班价格历史和当前报价生成简洁中文报告。",
                buildTimingPrompt(message, flight, history, trend)
        );
        String summary = aiSummary.orElseGet(() -> buildLocalSummary(flight, trend, history));
        return new TimingResponse(summary, trend.riskLevel(), trend.buyWindow(), flight, history);
    }

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

    private String buildLocalSummary(Flight flight, Trend trend, List<FlightPriceSnapshot> history) {
        String sampleText = history.size() < 2 ? "价格样本不足" : "价格正在" + trend.direction();
        return "本地分析：" + flight.flightNo() + " 当前价 " + flight.price().stripTrailingZeros().toPlainString()
                + " 元，" + sampleText + "，风险等级 " + trend.riskLevel() + "。" + trend.buyWindow();
    }

    private String buildTimingPrompt(String message, Flight flight, List<FlightPriceSnapshot> history, Trend trend) {
        return """
                用户需求：%s
                推荐航班：%s，%s到%s，起飞 %s，当前价 %s 元
                价格快照：%s
                本地趋势判断：%s，风险等级 %s，窗口建议：%s
                请输出 120 字以内购票时机报告。
                """.formatted(
                message,
                flight.flightNo(), flight.fromCity(), flight.toCity(), flight.departTime(),
                flight.price().stripTrailingZeros().toPlainString(),
                history,
                trend.direction(), trend.riskLevel(), trend.buyWindow()
        );
    }

    private record Trend(String riskLevel, String buyWindow, String direction) {
    }
}
