package com.sridhar.ragapi.service;

import com.sridhar.ragapi.entity.ChatMessage;
import com.sridhar.ragapi.entity.MessageRole;
import com.sridhar.ragapi.repository.ChatMessageRepository;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PostGresChatMemoryService implements ChatMemoryStore {
    private final ChatMessageRepository chatMessageRepository;
    private final SessionCacheService sessionCacheService;

    public PostGresChatMemoryService(ChatMessageRepository chatMessageRepository, SessionCacheService sessionCacheService) {
        this.chatMessageRepository = chatMessageRepository;
        this.sessionCacheService = sessionCacheService;
    }

    public List<dev.langchain4j.data.message.ChatMessage> getMessages(Object memoryId) {
        return chatMessageRepository
                .findBySessionIdOrderByCreatedAtAsc(memoryId.toString())
                .stream()
                .map(this::toL4jMessage)
                .collect(Collectors.toList());
    }

    @Transactional
    @Override
    public void updateMessages(Object memoryId,
                               List<dev.langchain4j.data.message.ChatMessage> messages) {
        chatMessageRepository.deleteBySessionId(memoryId.toString());
        messages.stream()
                .map(msg -> toEntity(memoryId.toString(), msg))
                .filter(entity -> entity != null)
                .forEach(chatMessageRepository::save);
        log.info("Updated {} messages for session {}", messages.size(), memoryId);
    }

    private ChatMessage toEntity(String sessionId,
                                 dev.langchain4j.data.message.ChatMessage msg) {
        if (msg instanceof SystemMessage) return null;
        if (msg instanceof ToolExecutionResultMessage) return null;

        MessageRole role = msg instanceof UserMessage ? MessageRole.USER : MessageRole.ASSISTANT;
        String intermediate = msg instanceof UserMessage
                ? ((UserMessage) msg).singleText()
                : ((AiMessage) msg).text();
        String content = intermediate != null ? intermediate : null;
                if(content == null) return null;
        int tokenCount = content.length() / 4;
        String userId = sessionCacheService
                .getUserIdBySession(sessionId)
                .orElse("unknown");
        return new ChatMessage(sessionId,userId, role, content,tokenCount);
    }

    @Transactional
    @Override
    public void deleteMessages(Object o) {
        chatMessageRepository.deleteBySessionId(o.toString());
        log.info("Deleted all messages for session {}", o);
    }

    private dev.langchain4j.data.message.ChatMessage toL4jMessage(ChatMessage entity) {
        return switch (entity.getRole()) {
            case USER -> UserMessage.from(entity.getContent());
            case ASSISTANT -> AiMessage.from(entity.getContent());
            case SYSTEM -> SystemMessage.from(entity.getContent());
        };
    }

}
