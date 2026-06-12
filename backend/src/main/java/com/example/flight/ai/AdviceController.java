package com.example.flight.ai;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AI 出行建议控制器 —— /api/ai 路径下的 REST API 入口。
 *
 * 提供的端点：
 * - POST /api/ai/advice           → 出行建议（单次查询）
 * - POST /api/ai/timing            → 购票时机分析
 * - POST /api/ai/conversations     → 创建多轮对话会话
 * - GET  /api/ai/conversations     → 列出最近会话
 * - GET  /api/ai/conversations/{sessionId}/messages  → 获取会话消息
 * - POST /api/ai/conversations/{sessionId}/messages  → 发送消息（含出行建议回复）
 * - DELETE /api/ai/conversations/{sessionId}         → 删除会话
 *
 * 所有方法统一通过 @Valid / @Validated 进行请求参数校验。
 */
@RestController
@RequestMapping("/api/ai")
@Validated
public class AdviceController {

    private static final Logger log = LoggerFactory.getLogger(AdviceController.class);

    private final AdviceService adviceService;
    private final TimingService timingService;
    private final ConversationRepository conversationRepository;

    /**
     * 构造函数注入三大依赖。
     *
     * @param adviceService          出行建议服务
     * @param timingService          购票时机分析服务
     * @param conversationRepository 对话持久化仓储
     */
    public AdviceController(AdviceService adviceService, TimingService timingService,
                            ConversationRepository conversationRepository) {
        this.adviceService = adviceService;
        this.timingService = timingService;
        this.conversationRepository = conversationRepository;
    }

    /**
     * POST /api/ai/advice —— 出行建议（单次查询，无会话上下文）。
     *
     * @param request 包含用户自然语言出行需求的请求体
     * @return AdviceResponse 包含 AI 摘要、推荐航班和候选航班列表
     */
    @PostMapping("/advice")
    public AdviceResponse advice(@Valid @RequestBody AdviceRequest request) {
        return adviceService.generate(request);
    }

    /**
     * POST /api/ai/timing —— 购票时机分析。
     *
     * @param request 包含用户购票时机问题的请求体
     * @return TimingResponse 包含分析摘要、风险等级、购票窗口等
     */
    @PostMapping("/timing")
    public TimingResponse timing(@Valid @RequestBody TimingRequest request) {
        return timingService.analyze(request);
    }

    /**
     * POST /api/ai/conversations —— 创建新的多轮对话会话。
     *
     * @param request 可选请求体，包含会话标题（为空时默认"新对话"）
     * @return 新创建的 ConversationSession
     */
    @PostMapping("/conversations")
    public ConversationSession createConversation(@RequestBody(required = false) ConversationRequest request) {
        String title = request != null && request.title() != null ? request.title() : "新对话";
        log.info("创建对话会话: title={}", title);
        return conversationRepository.createSession(title);
    }

    /**
     * GET /api/ai/conversations —— 列出最近创建的会话（最多20条）。
     *
     * @return 会话列表，按更新时间倒序
     */
    @GetMapping("/conversations")
    public List<ConversationSession> listConversations() {
        return conversationRepository.listSessions(20);
    }

    /**
     * GET /api/ai/conversations/{sessionId}/messages —— 获取指定会话的所有消息。
     *
     * @param sessionId 会话 ID
     * @return 消息列表，按创建时间正序排列
     */
    @GetMapping("/conversations/{sessionId}/messages")
    public ResponseEntity<List<ConversationMessage>> getMessages(@PathVariable String sessionId) {
        var messages = conversationRepository.getMessages(sessionId);
        return ResponseEntity.ok(messages);
    }

    /**
     * POST /api/ai/conversations/{sessionId}/messages —— 向会话发送消息并获取 AI 回复。
     *
     * 处理流程：
     * 1. 将用户消息写入 conversation_message（role=user）
     * 2. 加载该会话的完整历史消息
     * 3. 调用 AdviceService.generateInSession 生成带上下文的出行建议
     * 4. 将 AI 回复写入 conversation_message（role=assistant）
     * 5. 返回包含推荐结果的 AdviceResponse
     *
     * @param sessionId 会话 ID
     * @param request   包含消息内容的请求体
     * @return AdviceResponse 包含 AI 摘要和推荐航班
     */
    @PostMapping("/conversations/{sessionId}/messages")
    public ResponseEntity<AdviceResponse> sendMessage(@PathVariable String sessionId,
                                                       @Valid @RequestBody SendMessageRequest request) {
        log.info("对话消息: sessionId={}", sessionId);
        // 保存用户消息
        conversationRepository.addMessage(sessionId, "user", request.message());
        // 加载完整历史用于上下文理解
        List<ConversationMessage> history = conversationRepository.getMessages(sessionId);
        // 生成带上下文感知的出行建议
        AdviceResponse response = adviceService.generateInSession(sessionId, request.message(), history);
        // 保存 AI 回复
        conversationRepository.addMessage(sessionId, "assistant",
                response.summary() != null ? response.summary() : "已生成出行建议");
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/ai/conversations/{sessionId} —— 删除指定会话及其所有消息。
     *
     * @param sessionId 会话 ID
     * @return 204 No Content（删除成功）
     */
    @DeleteMapping("/conversations/{sessionId}")
    public ResponseEntity<Void> deleteConversation(@PathVariable String sessionId) {
        log.info("删除对话会话: sessionId={}", sessionId);
        conversationRepository.deleteSession(sessionId);
        return ResponseEntity.noContent().build();
    }
}
