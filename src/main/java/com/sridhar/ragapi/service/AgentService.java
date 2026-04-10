package com.sridhar.ragapi.service;

import com.sridhar.ragapi.agent.AgentContextHolder;
import com.sridhar.ragapi.agent.DocumentAssistant;
import com.sridhar.ragapi.council.CouncilOrchestrator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.api.common.OpenAiApiClientErrorException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

@Slf4j
@Service
public class AgentService {
    private final DocumentAssistant documentAssistant;
    private final CouncilOrchestrator council;
    private final SummaryService summaryService;
    private final PostGresChatMemoryService postGresChatMemoryService;

    public AgentService(DocumentAssistant documentAssistant,
                        CouncilOrchestrator council,
                        SummaryService summaryService, PostGresChatMemoryService postGresChatMemoryService) {
        this.documentAssistant = documentAssistant;
        this.council = council;
        this.summaryService = summaryService;
        this.postGresChatMemoryService = postGresChatMemoryService;
    }

    @Retryable(retryFor = {OpenAiApiClientErrorException.class, ResourceAccessException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2))
    public String agentChat(String sessionId,String userId,String userQuery){
        try {
            String draftAnswer = documentAssistant.chat(sessionId, userQuery);
            String context = AgentContextHolder.getContext();
            // Council activates only when the @Tool ran and captured RAG context
            if (context != null && !context.isBlank()) {
                draftAnswer = council.refine(userQuery, context, draftAnswer);
                postGresChatMemoryService.markLatestAssistantVerified(sessionId);
            }
            return draftAnswer;
        } finally {
            PostGresChatMemoryService.clearRequestCache();
            AgentContextHolder.clearContext();
            summaryService.compressIfNeeded(sessionId);
        }
    }

    @Recover
    public String fallbackAgentChat(Exception e,String sessionId,String userId,String userQuery){
        log.error("The AI Service is temporarily unavailable for session {}",e.getMessage());
        return "The AI service is temporarily unavailable. Please try again shortly.";
    }
}
