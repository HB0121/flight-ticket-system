package com.example.flight.ai;

import jakarta.validation.Valid;
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

    private final AdviceService adviceService;
    private final TimingService timingService;
    private final ConversationService conversationService;

    public AdviceController(AdviceService adviceService,
                            TimingService timingService,
                            ConversationService conversationService) {
        this.adviceService = adviceService;
        this.timingService = timingService;
        this.conversationService = conversationService;
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
        return conversationService.createConversation(request);
    }

    @GetMapping("/conversations")
    public List<ConversationSession> listConversations() {
        return conversationService.listConversations();
    }

    @GetMapping("/conversations/{sessionId}/messages")
    public ResponseEntity<List<ConversationMessage>> getMessages(@PathVariable String sessionId) {
        return ResponseEntity.ok(conversationService.getMessages(sessionId));
    }

    @PostMapping("/conversations/{sessionId}/messages")
    public ResponseEntity<AdviceResponse> sendMessage(@PathVariable String sessionId,
                                                       @Valid @RequestBody SendMessageRequest request) {
        return ResponseEntity.ok(conversationService.sendMessage(sessionId, request));
    }

    @DeleteMapping("/conversations/{sessionId}")
    public ResponseEntity<Void> deleteConversation(@PathVariable String sessionId) {
        conversationService.deleteConversation(sessionId);
        return ResponseEntity.noContent().build();
    }
}
