package com.sridhar.ragapi.service;

import com.sridhar.ragapi.entity.ChatMessage;
import com.sridhar.ragapi.entity.MessageRole;
import com.sridhar.ragapi.repository.ChatMessageRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class ChatHistoryService {
    private final ChatMessageRepository chatMessageRepository;

    public ChatHistoryService(ChatMessageRepository chatMessageRepository) {
        this.chatMessageRepository = chatMessageRepository;
    }

    @Transactional
    public void saveExchange(String sessionId, String userId, String userQuery, String assistantResponse) {
        chatMessageRepository.save(new ChatMessage(sessionId,userId, MessageRole.USER,userQuery,0));
        chatMessageRepository.save(new ChatMessage(sessionId,userId,MessageRole.ASSISTANT,assistantResponse,0));
    }
}
