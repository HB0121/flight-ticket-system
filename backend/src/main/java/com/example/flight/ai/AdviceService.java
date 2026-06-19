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
import java.util.Locale;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdviceService {

    private static final Logger log = LoggerFactory.getLogger(AdviceService.class);
    private static final Map<String, Set<String>> CITY_AIRPORT_ALIASES = Map.of(
            "沈阳", Set.of("SHE"),
            "重庆", Set.of("CKG"),
            "北京", Set.of("PEK", "PKX"),
            "上海", Set.of("PVG", "SHA"),
            "天津", Set.of("TSN")
    );

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
        String message = request == null ? "" : request.effectiveMessage();
        ParsedTravelIntent intent = TravelIntentParser.parse(message);
        AdviceIntentView intentView = AdviceIntentView.from(intent);
        CandidateSelection selection = selectCandidates(searchCandidates(intent), intent, message);
        List<Flight> candidates = selection.candidates();

        Optional<Flight> recommended = candidates.stream()
                .min(Comparator.comparing(Flight::price).thenComparing(Flight::departTime));

        if (recommended.isEmpty()) {
            String route = routeText(intent);
            log.info("出行建议: 未找到匹配航班, route={}", route);
            return new AdviceResponse(buildNoMatchSummary(route), intentView, null, candidates);
        }

        Flight flight = recommended.get();
        log.info("出行建议: 推荐航班 {}, 价格 {}", flight.flightNo(), flight.price());
        Optional<String> aiSummary = safeGenerateAdvice(
                "你是机票出行建议助手。基于候选航班给出简洁、可执行的中文建议。",
                buildAdvicePrompt(message, intent, flight, candidates, selection.timePreferenceMatched())
        );
        if (aiSummary.isPresent()) {
            log.debug("出行建议: 使用AI生成摘要");
            return new AdviceResponse(withTimePreferenceNote(aiSummary.get(), intent, message, selection.timePreferenceMatched()), intentView, flight, candidates);
        }

        log.debug("出行建议: 使用本地规则生成摘要");
        String summary = buildRuleSummary(intent, flight, message, selection.timePreferenceMatched());
        return new AdviceResponse(summary, intentView, flight, candidates);
    }

    public AdviceResponse generateInSession(String sessionId, String message, List<ConversationMessage> history) {
        ParsedTravelIntent intent = TravelIntentParser.parse(message);
        AdviceIntentView intentView = AdviceIntentView.from(intent);
        CandidateSelection selection = selectCandidates(searchCandidates(intent), intent, message);
        List<Flight> candidates = selection.candidates();

        Optional<Flight> recommended = candidates.stream()
                .min(Comparator.comparing(Flight::price).thenComparing(Flight::departTime));

        if (recommended.isEmpty()) {
            String route = routeText(intent);
            return new AdviceResponse(buildNoMatchSummary(route), intentView, null, candidates);
        }

        Flight flight = recommended.get();
        Optional<String> aiSummary = safeGenerateAdvice(
                "你是机票出行建议助手。根据对话历史和当前查询，给出简洁、连贯的中文建议。",
                buildConversationPrompt(message, intent, flight, candidates, history, selection.timePreferenceMatched())
        );
        String summary = aiSummary
                .map(value -> withTimePreferenceNote(value, intent, message, selection.timePreferenceMatched()))
                .orElseGet(() -> buildRuleSummary(intent, flight, message, selection.timePreferenceMatched()));
        return new AdviceResponse(summary, intentView, flight, candidates);
    }

    private List<Flight> searchCandidates(ParsedTravelIntent intent) {
        return flightSearchPort.search(new FlightSearchCriteria(null, null, intent.date()));
    }

    private CandidateSelection selectCandidates(List<Flight> flights, ParsedTravelIntent intent, String message) {
        List<Flight> baseMatches = flights.stream()
                .filter(flight -> matchesRoute(flight, intent))
                .filter(flight -> flight.seatsLeft() > 0)
                .filter(flight -> intent.budget() == null || flight.price().compareTo(intent.budget()) <= 0)
                .sorted(Comparator.comparing(Flight::price).thenComparing(Flight::departTime))
                .toList();

        if (intent.timePreference() == null) {
            return new CandidateSelection(limitCandidates(baseMatches), true);
        }

        List<Flight> exactMatches = baseMatches.stream()
                .filter(flight -> matchesTimePreference(flight, intent, message))
                .toList();

        if (!exactMatches.isEmpty()) {
            return new CandidateSelection(limitCandidates(exactMatches), true);
        }

        return new CandidateSelection(limitCandidates(baseMatches), false);
    }

    private List<Flight> limitCandidates(List<Flight> flights) {
        return flights.stream()
                .limit(5)
                .toList();
    }

    private boolean matchesRoute(Flight flight, ParsedTravelIntent intent) {
        return matchesLocation(intent.fromCity(), flight.fromAirport(), flight.fromCity())
                && matchesLocation(intent.toCity(), flight.toAirport(), flight.toCity());
    }

    private boolean matchesLocation(String city, String airportValue, String cityValue) {
        if (city == null || city.isBlank()) {
            return true;
        }

        Set<String> codes = CITY_AIRPORT_ALIASES.get(city);
        String normalizedAirport = normalizeAirportValue(airportValue);
        if (codes != null && normalizedAirport != null && codes.contains(normalizedAirport)) {
            return true;
        }

        return city.equals(cityValue);
    }

    private String normalizeAirportValue(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return normalized.length() == 3 ? normalized : null;
    }

    private boolean matchesTimePreference(Flight flight, ParsedTravelIntent intent, String message) {
        if (intent.timePreference() == null) {
            return true;
        }

        int hour = prefersArrivalWindow(message)
                ? flight.arriveTime().getHour()
                : flight.departTime().getHour();

        return switch (intent.timePreference()) {
            case EARLY_MORNING -> hour >= 0 && hour < 6;
            case MORNING -> hour >= 6 && hour < 12;
            case AFTERNOON -> hour >= 12 && hour < 18;
            case EVENING -> hour >= 18 && hour < 24;
        };
    }

    private boolean prefersArrivalWindow(String message) {
        return message != null && (
                message.contains("上午到")
                        || message.contains("下午到")
                        || message.contains("晚上到")
                        || message.contains("凌晨到")
                        || message.contains("早上到")
                        || message.contains("中午到")
                        || message.contains("到达")
        );
    }

    private String routeText(ParsedTravelIntent intent) {
        if (intent.fromCity() != null && intent.toCity() != null) {
            return intent.fromCity() + "到" + intent.toCity();
        }
        if (intent.toCity() != null) {
            return "前往" + intent.toCity();
        }
        return "";
    }

    private String withTimePreferenceNote(String summary, ParsedTravelIntent intent, String message, boolean timePreferenceMatched) {
        if (timePreferenceMatched || intent.timePreference() == null) {
            return summary;
        }
        return buildTimePreferenceFallbackNote(intent, message) + summary;
    }

    private Optional<String> safeGenerateAdvice(String systemPrompt, String userPrompt) {
        try {
            return aiTextClient.generate(systemPrompt, userPrompt);
        } catch (RuntimeException ex) {
            log.warn("出行建议: AI 调用失败，回退到规则建议: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private String buildNoMatchSummary(String route) {
        String routeText = route == null || route.isBlank() ? "" : route;
        return "当前本地数据库暂无符合条件的航班" + routeText + "。建议先同步航班数据，或放宽预算、日期和时间偏好后再试。";
    }

    private String buildRuleSummary(ParsedTravelIntent intent, Flight flight, String message, boolean timePreferenceMatched) {
        String budgetText = intent.budget() == null ? "当前最低价" : "预算内";
        String summary = "推荐 " + flight.flightNo() + "，" + flight.fromCity() + "到" + flight.toCity()
                + "，" + flight.departTime().toLocalDate() + " " + flight.departTime().toLocalTime()
                + " 起飞，价格" + flight.price().stripTrailingZeros().toPlainString()
                + " 元，属于" + budgetText + "选择。价格为系统本地价格快照，仅供课程设计演示参考。";
        return withTimePreferenceNote(summary, intent, message, timePreferenceMatched);
    }

    private String buildAdvicePrompt(String message, ParsedTravelIntent intent, Flight recommended, List<Flight> candidates,
                                     boolean timePreferenceMatched) {
        String budget = intent.budget() == null ? "未提供" : intent.budget().toPlainString() + "元";
        String timePreference = intent.timePreference() == null ? "未提供" : intent.timePreference().label();
        return String.join("\n",
                "用户需求：" + message,
                "解析结果：" + intent.fromCity() + " -> " + intent.toCity()
                        + "，日期" + intent.date() + "，预算" + budget + "，时间偏好" + timePreference,
                "时间偏好满足情况：" + (timePreferenceMatched ? "已匹配到符合时间偏好的航班" : buildTimePreferenceFallbackNote(intent, message)),
                "推荐航班：" + recommended.flightNo() + "，"
                        + recommended.fromAirport() + " -> " + recommended.toAirport()
                        + "，起飞" + recommended.departTime() + "，到达" + recommended.arriveTime()
                        + "，价格" + recommended.price().stripTrailingZeros().toPlainString()
                        + " 元，余票 " + recommended.seatsLeft(),
                "候选航班数量：" + candidates.size(),
                "请只基于这些候选航班，用80字以内给出推荐理由和购票建议。"
        );
    }

    private String buildConversationPrompt(String message, ParsedTravelIntent intent,
                                           Flight recommended, List<Flight> candidates,
                                           List<ConversationMessage> history,
                                           boolean timePreferenceMatched) {
        String budget = intent.budget() == null ? "未提供" : intent.budget().toPlainString() + "元";
        String timePreference = intent.timePreference() == null ? "未提供" : intent.timePreference().label();
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
                        + "，日期" + intent.date() + "，预算" + budget + "，时间偏好" + timePreference,
                "时间偏好满足情况：" + (timePreferenceMatched ? "已匹配到符合时间偏好的航班" : buildTimePreferenceFallbackNote(intent, message)),
                "推荐航班：" + recommended.flightNo() + "，"
                        + recommended.fromAirport() + " -> " + recommended.toAirport()
                        + "，起飞" + recommended.departTime()
                        + "，到达" + recommended.arriveTime()
                        + "，价格" + recommended.price().stripTrailingZeros().toPlainString()
                        + " 元，余票 " + recommended.seatsLeft(),
                "候选航班数量：" + candidates.size(),
                "请只基于候选航班和对话历史，用100字以内给出连贯的中文建议。"
        );
    }

    private String buildTimePreferenceFallbackNote(ParsedTravelIntent intent, String message) {
        String phrase = intent.timePreference().label() + (prefersArrivalWindow(message) ? "到达" : "出发");
        return "暂无完全符合" + phrase + "的航班，以下为同日期同路线的相近可选航班。";
    }

    private record CandidateSelection(List<Flight> candidates, boolean timePreferenceMatched) {
    }
}
