package com.sridhar.ragapi.service;

import com.sridhar.ragapi.agent.DocumentAssistant;
import org.springframework.stereotype.Service;

@Service
public class AgentService {
    private final ChatHistoryService chatHistoryService;
    private final DocumentAssistant documentAssistant;


    public AgentService(ChatHistoryService chatHistoryService, DocumentAssistant documentAssistant) {
        this.chatHistoryService = chatHistoryService;
        this.documentAssistant = documentAssistant;
    }

    public String agentChat(String sessionId,String userId,String userQuery){
        String response = documentAssistant.chat(sessionId,userQuery);
        chatHistoryService.saveExchange(sessionId,userId,userQuery,response);
        return response;
    }
}
