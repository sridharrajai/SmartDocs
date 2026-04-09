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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PostGresChatMemoryService implements ChatMemoryStore {
    private final ChatMessageRepository chatMessageRepository;
    private final SessionCacheService sessionCacheService;

    /**
     * Per-request cache that holds the full message list including tool messages.
     *
     * MessageWindowChatMemory calls getMessages() on every add() — it has no
     * internal list of its own. Without this cache, tool messages filtered from the
     * DB would be invisible to the LLM mid-request, causing it to call the tool
     * again in a loop. The cache is cleared after each agent call via clearRequestCache().
     */
    private static final ThreadLocal<Map<String, List<dev.langchain4j.data.message.ChatMessage>>> REQUEST_CACHE =
            ThreadLocal.withInitial(HashMap::new);

    public PostGresChatMemoryService(ChatMessageRepository chatMessageRepository, SessionCacheService sessionCacheService) {
        this.chatMessageRepository = chatMessageRepository;
        this.sessionCacheService = sessionCacheService;
    }

    @Override
    public List<dev.langchain4j.data.message.ChatMessage> getMessages(Object memoryId) {
        String id = memoryId.toString();
        Map<String, List<dev.langchain4j.data.message.ChatMessage>> cache = REQUEST_CACHE.get();
        if (cache.containsKey(id)) {
            return new ArrayList<>(cache.get(id));
        }
        List<dev.langchain4j.data.message.ChatMessage> messages = chatMessageRepository
                .findBySessionIdOrderByCreatedAtAsc(id)
                .stream()
                .map(this::toL4jMessage)
                .collect(Collectors.toList());
        cache.put(id, new ArrayList<>(messages));
        return messages;
    }

    @Transactional
    @Override
    public void updateMessages(Object memoryId,
                               List<dev.langchain4j.data.message.ChatMessage> messages) {
        String id = memoryId.toString();
        // Keep full message list (tool messages included) in the request-scoped cache
        REQUEST_CACHE.get().put(id, new ArrayList<>(messages));
        // Persist only user + final assistant messages to DB
        chatMessageRepository.deleteBySessionId(id);
        messages.stream()
                .map(msg -> toEntity(id, msg))
                .filter(entity -> entity != null)
                .forEach(chatMessageRepository::save);
        log.info("Updated {} messages for session {}", messages.size(), memoryId);
    }

    @Transactional
    @Override
    public void deleteMessages(Object o) {
        String id = o.toString();
        REQUEST_CACHE.get().remove(id);
        chatMessageRepository.deleteBySessionId(id);
        log.info("Deleted all messages for session {}", o);
    }

    /**
     * Must be called after each agent chat invocation to prevent the cache from
     * leaking into the next request on the same thread.
     */
    public static void clearRequestCache() {
        REQUEST_CACHE.remove();
    }

    private ChatMessage toEntity(String sessionId,
                                 dev.langchain4j.data.message.ChatMessage msg) {
        if (msg instanceof ToolExecutionResultMessage) return null;
        if (msg instanceof AiMessage ai && ai.hasToolExecutionRequests()) return null;
        if (msg instanceof SystemMessage sm && !sm.text().startsWith("Summary of earlier conversation:")) return null;

        MessageRole role;
        String intermediate;
        if (msg instanceof UserMessage) {
            role = MessageRole.USER;
            intermediate = ((UserMessage) msg).singleText();
        } else if (msg instanceof SystemMessage) {
            role = MessageRole.SYSTEM;
            intermediate = ((SystemMessage) msg).text();
        } else {
            role = MessageRole.ASSISTANT;
            intermediate = ((AiMessage) msg).text();
        }
        String content = intermediate != null ? intermediate : null;
        if (content == null) return null;
        int tokenCount = content.length() / 4;
        String userId = sessionCacheService
                .getUserIdBySession(sessionId)
                .orElse("unknown");
        return new ChatMessage(sessionId, userId, role, content, tokenCount);
    }

    private dev.langchain4j.data.message.ChatMessage toL4jMessage(ChatMessage entity) {
        return switch (entity.getRole()) {
            case USER -> UserMessage.from(entity.getContent());
            case ASSISTANT -> AiMessage.from(entity.getContent());
            case SYSTEM -> SystemMessage.from(entity.getContent());
        };
    }
}
