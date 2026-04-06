package com.sridhar.ragapi.service;

import com.sridhar.ragapi.agent.AgentContextHolder;
import com.sridhar.ragapi.agent.DocumentAssistant;
import com.sridhar.ragapi.council.CouncilOrchestrator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.api.common.OpenAiApiClientErrorException;
import org.springframework.context.annotation.Fallback;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

@Slf4j
@Service
public class AgentService {
    private final ChatHistoryService chatHistoryService;
    private final DocumentAssistant documentAssistant;
    private final TokenAwareMemoryService tokenAwareMemoryService;
    private final CouncilOrchestrator council;




    public AgentService(ChatHistoryService chatHistoryService, DocumentAssistant documentAssistant, TokenAwareMemoryService tokenAwareMemoryService, CouncilOrchestrator council) {
        this.chatHistoryService = chatHistoryService;
        this.documentAssistant = documentAssistant;
        this.tokenAwareMemoryService = tokenAwareMemoryService;
        this.council = council;
    }

    @Retryable(retryFor = {OpenAiApiClientErrorException.class, ResourceAccessException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2))
    public String agentChat(String sessionId,String userId,String userQuery){
        try {
            String draftAnswer = documentAssistant.chat(sessionId, userQuery);
            String context = AgentContextHolder.getContext();
            // Council activates only when the @Tool ran and captured RAG context
            if (context != null && !context.isBlank())
                draftAnswer = council.refine(userQuery, context, draftAnswer);
            chatHistoryService.saveExchange(sessionId, userId, userQuery,
                    draftAnswer);
            return draftAnswer;
        }finally {
            AgentContextHolder.clearContext();
        }
    }

    @Recover
    public String fallbackAgentChat(Exception e,String sessionId,String userId,String userQuery){
        log.error("The AI Service is temporarily unavailable for session {}",e.getMessage());
        return "The AI service is temporarily unavailable. Please try again shortly.";
    }
}
