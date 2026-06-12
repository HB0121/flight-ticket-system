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

@RestController
@RequestMapping("/api/ai")
@Validated
public class AdviceController {

    private static final Logger log = LoggerFactory.getLogger(AdviceController.class);

    private final AdviceService adviceService;
    private final TimingService timingService;
    private final ConversationRepository conversationRepository;

    public AdviceController(AdviceService adviceService, TimingService timingService,
                            ConversationRepository conversationRepository) {
        this.adviceService = adviceService;
        this.timingService = timingService;
        this.conversationRepository = conversationRepository;
    }

    @PostMapping("/advice")
    public AdviceResponse advice(@Valid @RequestBody AdviceRequest request) {
        return adviceService.generate(request);
    }

    @PostMapping("/timing")
    public TimingResponse timing(@Valid @RequestBody TimingRequest request) {
        return timingService.analyze(request);
    }

    @PostMapping("/conversations")
    public ConversationSession createConversation(@RequestBody(required = false) ConversationRequest request) {
        String title = request != null && request.title() != null ? request.title() : "新对话";
        log.info("创建对话会话: title={}", title);
        return conversationRepository.createSession(title);
    }

    @GetMapping("/conversations")
    public List<ConversationSession> listConversations() {
        return conversationRepository.listSessions(20);
    }

    @GetMapping("/conversations/{sessionId}/messages")
    public ResponseEntity<List<ConversationMessage>> getMessages(@PathVariable String sessionId) {
        var messages = conversationRepository.getMessages(sessionId);
        return ResponseEntity.ok(messages);
    }

    @PostMapping("/conversations/{sessionId}/messages")
    public ResponseEntity<AdviceResponse> sendMessage(@PathVariable String sessionId,
                                                       @Valid @RequestBody SendMessageRequest request) {
        log.info("对话消息: sessionId={}", sessionId);
        conversationRepository.addMessage(sessionId, "user", request.message());
        List<ConversationMessage> history = conversationRepository.getMessages(sessionId);
        AdviceResponse response = adviceService.generateInSession(sessionId, request.message(), history);
        conversationRepository.addMessage(sessionId, "assistant",
                response.summary() != null ? response.summary() : "已生成出行建议");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/conversations/{sessionId}")
    public ResponseEntity<Void> deleteConversation(@PathVariable String sessionId) {
        log.info("删除对话会话: sessionId={}", sessionId);
        conversationRepository.deleteSession(sessionId);
        return ResponseEntity.noContent().build();
    }
}
