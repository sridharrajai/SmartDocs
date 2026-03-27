package com.sridhar.ragapi.service;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ChatService {
    private final ChatClient chatClient;
    public ChatService(OpenAiChatModel chatmodel) {
        this.chatClient = ChatClient.create(chatmodel);
    }
    @PostConstruct
    public void init() { log.info("ChatService bean ready"); }

    public String chat(String message) {
        log.info("ChatService received message  {}", message);
        return chatClient.prompt().user(message).call().content().strip();
    }
}
