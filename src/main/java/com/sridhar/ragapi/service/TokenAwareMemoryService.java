package com.sridhar.ragapi.service;

import com.sridhar.ragapi.entity.ChatMessage;
import com.sridhar.ragapi.repository.ChatMessageRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

@Service
public class TokenAwareMemoryService {
    private final int MAX_TOKEN_LIMIT = 3000;
    private final int CHARS_PER_TOKEN = 4;
    private final ChatMessageRepository chatMessageRepository;

    public TokenAwareMemoryService(ChatMessageRepository chatMessageRepository) {
        this.chatMessageRepository = chatMessageRepository;
    }

    public List<ChatMessage> trimmedMemory(String sessionId){
        int tokenBudget = MAX_TOKEN_LIMIT;
        List<ChatMessage> fullMessages = chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        Deque<ChatMessage> trimmedMessages = new ArrayDeque<>();
        for(int i=fullMessages.size()-1; i>=0; i--){
            ChatMessage chatMessage = fullMessages.get(i);
            int estimate =chatMessage.getContent().length()/CHARS_PER_TOKEN;
            if (tokenBudget - estimate < 0) break;
            trimmedMessages.addFirst(chatMessage);
            tokenBudget-=estimate;
        }
        return new ArrayList<>(trimmedMessages);
    }
}
