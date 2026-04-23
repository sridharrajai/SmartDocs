package com.sridhar.ragapi.service;

import com.sridhar.ragapi.agent.AgentContextHolder;
import com.sridhar.ragapi.agent.DocumentAssistant;
import com.sridhar.ragapi.council.CouncilOrchestrator;
import com.sridhar.ragapi.exception.PromptInjectionException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
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
    private final Timer llmTimer;
    private final Counter tokenCounter;


    public AgentService(DocumentAssistant documentAssistant,
                        CouncilOrchestrator council,
                        SummaryService summaryService, PostGresChatMemoryService postGresChatMemoryService, MeterRegistry registry) {
        this.documentAssistant = documentAssistant;
        this.council = council;
        this.summaryService = summaryService;
        this.postGresChatMemoryService = postGresChatMemoryService;
        this.llmTimer = Timer.builder("ai.llm.latency")
                .description("Time spent waiting for LLM API response")
                .register(registry);
        this.tokenCounter = Counter.builder("ai.tokens.total")
                .description("Total LLM tokens consumed across all sessions")
                .register(registry);
    }

    @Retryable(retryFor = {OpenAiApiClientErrorException.class, ResourceAccessException.class},
            noRetryFor = {PromptInjectionException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2))
    public String agentChat(String sessionId,String userId,String userQuery) {
        try {
            String draftAnswer = llmTimer.record(()->
                    documentAssistant.chat(sessionId, userQuery));
            tokenCounter.increment(draftAnswer.length() / 4.0);
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
        log.error("All retries exhausted for session {}", sessionId, e);
        return "The AI service is temporarily unavailable. Please try again shortly.";
    }
}
