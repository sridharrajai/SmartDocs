package com.sridhar.ragapi.controller;

import com.sridhar.ragapi.util.AgentRequest;
import com.sridhar.ragapi.service.AgentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/agent")
public class AgentController {
    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping("/chat")
    public ResponseEntity<String> agentChat(@RequestHeader("x-session-id") String sessionId, @RequestHeader("x-user-id") String userId,@RequestBody AgentRequest request){
     log.info("Received chat request - sessionId: {}, userId: {}, question: {}", sessionId, userId, request.userQuery());
        return ResponseEntity.ok(agentService.agentChat(sessionId,userId,request.userQuery()));
    }
}
