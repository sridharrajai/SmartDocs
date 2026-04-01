package com.sridhar.ragapi.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class CouncilOrchestrator {
    private final ChatClient chatClient;

    public CouncilOrchestrator(ChatClient.Builder chatClient) {
        this.chatClient = chatClient.build();
    }

    public String refine(String originalQuery, String retrievedChunks,String draftResponse){
        return draftResponse;
    }
}
