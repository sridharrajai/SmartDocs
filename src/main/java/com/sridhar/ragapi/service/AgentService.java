package com.sridhar.ragapi.service;

import com.sridhar.ragapi.agent.AgentContextHolder;
import com.sridhar.ragapi.agent.DocumentAssistant;
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



    public AgentService(ChatHistoryService chatHistoryService, DocumentAssistant documentAssistant) {
        this.chatHistoryService = chatHistoryService;
        this.documentAssistant = documentAssistant;
    }

    @Retryable(retryFor = {OpenAiApiClientErrorException.class, ResourceAccessException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2))
    public String agentChat(String sessionId,String userId,String userQuery){
        AgentContextHolder.clearContext();
        String response = documentAssistant.chat(sessionId,userQuery);
        chatHistoryService.saveExchange(sessionId,userId,userQuery,response);
        AgentContextHolder.clearContext();
        return response;
    }

    @Recover
    public String fallbackAgentChat(Exception e,String sessionId,String userId,String userQuery){
        log.error("The AI Service is temporarily unavailable for session {}",sessionId,e.getMessage());
        return "The AI service is temporarily unavailable. Please try again shortly.";
    }
}
