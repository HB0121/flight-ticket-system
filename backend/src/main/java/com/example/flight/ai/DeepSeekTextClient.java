package com.example.flight.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * DeepSeek AI 文本生成客户端 —— {@link AiTextClient} 接口的唯一实现。
 *
 * 职责：调用 DeepSeek Chat Completions API（兼容 OpenAI 格式），将原始响应
 * 中的 choices[0].message.content 提取出来。
 *
 * 降级策略：
 * - 如果 API key 未配置（空字符串），直接返回 Optional.empty()；
 * - 任何网络异常或响应解析失败时捕获异常并返回 Optional.empty()；
 * - 上游服务（AdviceService / TimingService）检测到 empty 后自动使用本地规则引擎。
 *
 * 这种设计保证系统在无外网、无 API key 的教室演示环境下仍然完全可用。
 */
@Component
public class DeepSeekTextClient implements AiTextClient {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekTextClient.class);
    /** DeepSeek API 密钥，从配置项 app.deepseek.api-key 注入，默认为空 */
    private final String apiKey;
    /** 模型名称，从配置项 app.deepseek.model 注入，默认为 deepseek-v4-flash */
    private final String model;
    /** RestClient 实例，预配置了 Base URL 和 Authorization 头 */
    private final RestClient restClient;

    /**
     * 构造函数 —— 通过 Spring 属性注入配置参数并构建 RestClient。
     *
     * @param apiKey  DeepSeek API 密钥
     * @param baseUrl DeepSeek API 基础地址，默认 https://api.deepseek.com
     * @param model   模型名称，默认 deepseek-v4-flash
     */
    public DeepSeekTextClient(@Value("${app.deepseek.api-key:}") String apiKey,
                              @Value("${app.deepseek.base-url:https://api.deepseek.com}") String baseUrl,
                              @Value("${app.deepseek.model:deepseek-v4-flash}") String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    /**
     * 调用 DeepSeek Chat Completions API 生成文本。
     *
     * @param systemPrompt 系统提示词，定义 AI 角色（如"你是机票出行建议助手"）
     * @param userPrompt   用户提示词，包含具体的数据和问题
     * @return AI 生成的文本；如果 API key 未配置或调用失败，返回 Optional.empty()
     */
    @Override
    public Optional<String> generate(String systemPrompt, String userPrompt) {
        // 如果 API key 未配置，直接降级，不发起网络请求
        if (!StringUtils.hasText(apiKey)) {
            log.debug("DeepSeek API key not configured, using local fallback");
            return Optional.empty();
        }
        try {
            // 构造请求体：模型名 + messages 列表（system + user）+ 非流式
            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)
                    ),
                    "stream", false
            );
            // 发送 POST 请求到 /chat/completions 端点
            Map<?, ?> response = restClient.post()
                    .uri("/chat/completions")
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            return extractContent(response);
        } catch (Exception e) {
            // 任何异常（网络超时、4xx、5xx 等）都静默降级
            log.warn("DeepSeek API调用失败: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 从 DeepSeek API 响应中提取 choices[0].message.content 文本。
     *
     * 响应 JSON 格式示例：
     * {"choices": [{"message": {"content": "建议您..."}}]}
     *
     * 对每一步的类型转换都做防御性检查，防止 NPE 或 ClassCastException。
     *
     * @param response API 响应的 Map 表示
     * @return 提取的文本内容，解析失败返回 Optional.empty()
     */
    private Optional<String> extractContent(Map<?, ?> response) {
        if (response == null) {
            return Optional.empty();
        }
        // 提取 choices 数组
        Object choicesValue = response.get("choices");
        if (!(choicesValue instanceof List<?> choices) || choices.isEmpty()) {
            return Optional.empty();
        }
        // 提取第一个 choice
        Object firstChoice = choices.get(0);
        if (!(firstChoice instanceof Map<?, ?> choice)) {
            return Optional.empty();
        }
        // 提取 message 对象
        Object messageValue = choice.get("message");
        if (!(messageValue instanceof Map<?, ?> message)) {
            return Optional.empty();
        }
        // 提取 content 字符串
        Object content = message.get("content");
        return content == null || !StringUtils.hasText(content.toString())
                ? Optional.empty()
                : Optional.of(content.toString().trim());
    }
}
