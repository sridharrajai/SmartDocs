package com.sridhar.ragapi.config;

import com.sridhar.ragapi.agent.KnowledgeBaseTool;
import com.sridhar.ragapi.repository.ChatMessageRepository;
import com.sridhar.ragapi.repository.IngestedDocumentRepository;
import com.sridhar.ragapi.service.PostGresChatMemoryService;
import com.sridhar.ragapi.service.TokenAwareMemoryService;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LangChain4jConfig {

    @Bean(name = "langchain4jChatModel")
    public ChatModel langchain4jChatModel(
            @Value("${spring.ai.openai.api-key}") String apiKey) {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4o-mini")
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    @Bean(name = "langchain4jChatMemoryProvider")
    public ChatMemoryProvider chatMemoryProvider(PostGresChatMemoryService postGresChatMemoryService) {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(20)
                .chatMemoryStore(postGresChatMemoryService)
                .build();
    }

    @Bean
    public KnowledgeBaseTool knowledgeBaseTool(VectorStore vectorStore, IngestedDocumentRepository ingestedDocumentRepository) {
        return new KnowledgeBaseTool(vectorStore, ingestedDocumentRepository);
    }

    @Bean
    public TokenAwareMemoryService tokenAwareMemoryService(ChatMessageRepository chatMessageRepository) {
        return new TokenAwareMemoryService(chatMessageRepository);
    }
}
