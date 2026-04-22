package com.sridhar.ragapi.controller;

import com.sridhar.ragapi.exception.PromptInjectionException;
import com.sridhar.ragapi.service.AgentService;
import com.sridhar.ragapi.service.SessionManager;
import com.sridhar.ragapi.util.AgentRequest;
import com.sridhar.ragapi.util.PromptInjectionFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@Slf4j
@RestController
@RequestMapping("/api/v1/agent")
public class AgentController {
    private final AgentService agentService;
    private final SessionManager sessionManager;
    private final ChatClient chatClient;
    private final PromptInjectionFilter promptInjectionFilter ;

    public AgentController(AgentService agentService, SessionManager sessionManager, OpenAiChatModel chatModel, PromptInjectionFilter promptInjectionFilter) {
        this.chatClient = ChatClient.create( chatModel);
        this.agentService = agentService;
        this.sessionManager = sessionManager;
        this.promptInjectionFilter = promptInjectionFilter;
    }

    @PostMapping("/chat")
    public ResponseEntity<String> agentChat(@RequestHeader("x-session-id") String sessionId, @RequestHeader("x-user-id") String userId,@RequestBody AgentRequest request) throws PromptInjectionException {
        String session = sessionManager.getOrCreateSession(userId, sessionId);
        promptInjectionFilter.validate(request.userQuery());
        log.info("Received chat request - sessionId: {}, userId: {}, question: {}", sessionId, userId, request.userQuery());
        return ResponseEntity.ok(agentService.agentChat(session,userId,request.userQuery()));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(
            @RequestParam String message,
            @RequestHeader(value="X-Session-Id", defaultValue="default") String
                    sessionId) {
        return chatClient.prompt().user(message).stream().content();
    }
}
