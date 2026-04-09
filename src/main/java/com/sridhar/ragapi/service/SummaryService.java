package com.sridhar.ragapi.service;

import com.sridhar.ragapi.agent.SummaryAgent;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SummaryService {
    private final SummaryAgent summaryAgent;
    private final PostGresChatMemoryService postGresChatMemoryService;
    private final TokenAwareMemoryService tokenAwareMemoryService;

    public SummaryService(SummaryAgent summaryAgent,
                          PostGresChatMemoryService postGresChatMemoryService,
                          TokenAwareMemoryService tokenAwareMemoryService) {
        this.summaryAgent = summaryAgent;
        this.postGresChatMemoryService = postGresChatMemoryService;
        this.tokenAwareMemoryService = tokenAwareMemoryService;
    }

    public void compressIfNeeded(String sessionId) {
        List<ChatMessage> allMessages = postGresChatMemoryService.getMessages(sessionId);
        List<com.sridhar.ragapi.entity.ChatMessage> trimmedMessages = tokenAwareMemoryService.trimmedMemory(sessionId);
        log.info("compressIfNeeded: all={}, trimmed={}", allMessages.size(), trimmedMessages.size());

        if (allMessages.size() <= trimmedMessages.size()) return;

        int trimCount = allMessages.size() - trimmedMessages.size();
        List<ChatMessage> toSummarise = allMessages.subList(0, trimCount);

        String conversationText = toSummarise.stream()
                .map(msg -> switch (msg.type()) {
                    case USER -> "USER: " + ((UserMessage) msg).singleText();
                    case AI -> "ASSISTANT: " + ((AiMessage) msg).text();
                    case SYSTEM -> "SYSTEM: " + ((SystemMessage) msg).text();
                    default -> "";
                })
                .filter(s -> !s.isBlank())
                .collect(Collectors.joining("\n"));

        String summaryText = summaryAgent.summarise(conversationText);
        log.info("Summary generated for session {}", sessionId);

        List<ChatMessage> compressed = new ArrayList<>();
        compressed.add(SystemMessage.from("Summary of earlier conversation: " + summaryText));
        compressed.addAll(allMessages.subList(trimCount, allMessages.size()));

        postGresChatMemoryService.updateMessages(sessionId, compressed);
    }
}
