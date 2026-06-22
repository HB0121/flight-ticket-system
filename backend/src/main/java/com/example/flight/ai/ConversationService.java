package com.example.flight.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConversationService {

    private static final Logger log = LoggerFactory.getLogger(ConversationService.class);
    private static final int DEFAULT_SESSION_LIMIT = 20;
    private static final String DEFAULT_TITLE = "鏂板璇?";
    private static final String DEFAULT_ASSISTANT_MESSAGE = "宸茬敓鎴愬嚭琛屽缓璁?";

    private final ConversationRepository conversationRepository;
    private final AdviceService adviceService;

    public ConversationService(ConversationRepository conversationRepository,
                               AdviceService adviceService) {
        this.conversationRepository = conversationRepository;
        this.adviceService = adviceService;
    }

    public ConversationSession createConversation(ConversationRequest request) {
        String title = request != null && request.title() != null ? request.title() : DEFAULT_TITLE;
        log.info("鍒涘缓瀵硅瘽浼氳瘽: title={}", title);
        return conversationRepository.createSession(title);
    }

    public List<ConversationSession> listConversations() {
        return conversationRepository.listSessions(DEFAULT_SESSION_LIMIT);
    }

    public List<ConversationMessage> getMessages(String sessionId) {
        return conversationRepository.getMessages(sessionId);
    }

    public AdviceResponse sendMessage(String sessionId, SendMessageRequest request) {
        log.info("瀵硅瘽娑堟伅: sessionId={}", sessionId);
        conversationRepository.addMessage(sessionId, "user", request.message());
        List<ConversationMessage> history = conversationRepository.getMessages(sessionId);
        AdviceResponse response = adviceService.generateInSession(sessionId, request.message(), history);
        conversationRepository.addMessage(sessionId, "assistant",
                response.summary() != null ? response.summary() : DEFAULT_ASSISTANT_MESSAGE);
        return response;
    }

    public void deleteConversation(String sessionId) {
        log.info("鍒犻櫎瀵硅瘽浼氳瘽: sessionId={}", sessionId);
        conversationRepository.deleteSession(sessionId);
    }
}
