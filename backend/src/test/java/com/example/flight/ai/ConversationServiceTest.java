package com.example.flight.ai;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConversationServiceTest {

    @Test
    void createsConversationWithRequestedTitle() {
        ConversationRepository conversationRepository = mock(ConversationRepository.class);
        AdviceService adviceService = mock(AdviceService.class);
        ConversationService service = new ConversationService(conversationRepository, adviceService);
        ConversationSession created = session("session-1", "Trip plan");
        when(conversationRepository.createSession("Trip plan")).thenReturn(created);

        ConversationSession result = service.createConversation(new ConversationRequest("Trip plan"));

        assertThat(result).isSameAs(created);
        verify(conversationRepository).createSession("Trip plan");
    }

    @Test
    void sendsMessageAndPersistsAssistantSummary() {
        ConversationRepository conversationRepository = mock(ConversationRepository.class);
        AdviceService adviceService = mock(AdviceService.class);
        ConversationService service = new ConversationService(conversationRepository, adviceService);
        List<ConversationMessage> history = List.of(message("user", "hello"));
        AdviceResponse response = new AdviceResponse("summary text", null, null, List.of());
        when(conversationRepository.getMessages("session-1")).thenReturn(history);
        when(adviceService.generateInSession("session-1", "hello", history)).thenReturn(response);

        AdviceResponse result = service.sendMessage("session-1", new SendMessageRequest("hello"));

        assertThat(result).isSameAs(response);
        var inOrder = inOrder(conversationRepository, adviceService);
        inOrder.verify(conversationRepository).addMessage("session-1", "user", "hello");
        inOrder.verify(conversationRepository).getMessages("session-1");
        inOrder.verify(adviceService).generateInSession("session-1", "hello", history);
        inOrder.verify(conversationRepository).addMessage("session-1", "assistant", "summary text");
    }

    @Test
    void deletesConversationBySessionId() {
        ConversationRepository conversationRepository = mock(ConversationRepository.class);
        AdviceService adviceService = mock(AdviceService.class);
        ConversationService service = new ConversationService(conversationRepository, adviceService);

        service.deleteConversation("session-1");

        verify(conversationRepository).deleteSession("session-1");
    }

    private static ConversationSession session(String id, String title) {
        LocalDateTime now = LocalDateTime.parse("2026-06-22T10:00:00");
        return new ConversationSession(id, title, now, now);
    }

    private static ConversationMessage message(String role, String content) {
        return new ConversationMessage(1L, "session-1", role, content, LocalDateTime.parse("2026-06-22T10:00:00"));
    }
}
