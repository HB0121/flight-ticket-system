package com.example.flight.ai;

import java.util.Optional;

/**
 * AI 文本生成客户端接口 —— 函数式接口（FunctionalInterface）。
 *
 * 设计意图：将 AI 调用抽象为单一方法，便于在 AdviceService / TimingService 中
 * 实现"优雅降级"策略 —— 当外部 AI（如 DeepSeek）不可用时，通过 lambda 注入一个
 * 直接返回 Optional.empty() 的实例，上游服务自动回退到本地规则引擎。
 *
 * 这种基于接口隔离的降级模式避免了到处写 if-else 判空，符合"端口-适配器"架构思想。
 */
@FunctionalInterface
public interface AiTextClient {
    /**
     * 根据系统提示词和用户提示词生成 AI 文本回复。
     *
     * @param systemPrompt 系统提示词，定义 AI 的角色和行为
     * @param userPrompt   用户提示词，包含具体的问题和上下文数据
     * @return AI 生成的文本（可能为空，表示 AI 不可用，调用方应回退到本地逻辑）
     */
    Optional<String> generate(String systemPrompt, String userPrompt);
}
