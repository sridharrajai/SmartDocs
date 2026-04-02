package com.sridhar.ragapi.agent;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

@AiService(wiringMode = AiServiceWiringMode.EXPLICIT,chatModel = "langchain4jChatModel", chatMemoryProvider = "langchain4jChatMemoryProvider")
public interface DocumentAssistant {
    @SystemMessage("You are a helpful assistant that answers user queries based on the provided document chunks. " +
            "Use the retrieved document chunks to answer the user's question. " +
            "If the retrieved information is insufficient, respond with 'I don't know.'")
    public String chat(@MemoryId String sessionId,@UserMessage String userQuery) ;
}
