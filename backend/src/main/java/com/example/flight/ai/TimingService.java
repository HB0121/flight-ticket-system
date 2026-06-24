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

@Service
public class TimingService {

    private static final Logger log = LoggerFactory.getLogger(TimingService.class);
    private final FlightSearchPort flightSearchPort;
    private final PriceHistoryPort priceHistoryPort;
    private final AiTextClient aiTextClient;
    private final PriceContextRepository priceContextRepository;

    public TimingService(FlightSearchPort flightSearchPort,
                         PriceHistoryPort priceHistoryPort,
                         AiTextClient aiTextClient,
                         PriceContextRepository priceContextRepository) {
        this.flightSearchPort = flightSearchPort;
        this.priceHistoryPort = priceHistoryPort;
        this.aiTextClient = aiTextClient;
        this.priceContextRepository = priceContextRepository;
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
        log.info("购票时机分析: 航班 {}, 历史快照数 {}, 当前价 {}", flight.flightNo(), history.size(), flight.price());
        Trend trend = analyzeTrend(history);
        log.debug("购票时机分析: 趋势 direction={}, riskLevel={}", trend.direction(), trend.riskLevel());

        // RAG: retrieve price context for this route
        List<String> ragContexts = priceContextRepository.searchContext(flight.fromCity(), flight.toCity());
        final List<String> finalRagContexts = ragContexts.isEmpty()
                ? priceContextRepository.searchByKeyword(flight.fromCity() + " " + flight.toCity())
                : ragContexts;
        log.debug("购票时机分析: RAG检索到{}条上下文", finalRagContexts.size());

        // Holiday proximity
        final String holidayInfo = intent.date() != null
                ? HolidayProximityCalculator.holidayAdvice(intent.date())
                : "";

        Optional<String> aiSummary = aiTextClient.generate(
                "你是机票购票时机分析助手。根据航班价格历史、历史规律和节假日信息，生成简洁中文购票时机报告。",
                buildTimingPrompt(message, flight, history, trend, finalRagContexts, holidayInfo)
        );
        String summary = aiSummary.orElseGet(() -> buildLocalSummary(flight, trend, history, finalRagContexts, holidayInfo));
        log.info("购票时机分析完成: riskLevel={}", trend.riskLevel());
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

        if (!holidayInfo.isBlank()) {
            sb.append(" ").append(holidayInfo);
        }

        if (!ragContexts.isEmpty()) {
            sb.append(" 参考历史规律：").append(ragContexts.get(0).length() > 100
                    ? ragContexts.get(0).substring(0, 100) + "..."
                    : ragContexts.get(0));
        }

        sb.append(" ").append(trend.buyWindow());
        return sb.toString();
    }

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

    private record Trend(String riskLevel, String buyWindow, String direction) {
    }
}
