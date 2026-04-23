package com.sridhar.ragapi.service;
import com.sridhar.ragapi.util.AskRequest;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStoreRetriever;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChatService {
    private final ChatClient chatClient;
    private final VectorStoreRetriever vectorStore;
    public ChatService(OpenAiChatModel chatModel, VectorStoreRetriever vectorStore) {
        this.chatClient = ChatClient.create(chatModel);
        this.vectorStore= vectorStore;
    }
    @PostConstruct
    public void init() {
        log.info("ChatService bean ready");
        log.info("Running on virtual thread: {}", Thread.currentThread().isVirtual());
    }

    @Cacheable(value = "chat-responses", key = "#message")
    public String chat(String message) {
        log.info("ChatService received message  {}", message);
        return chatClient.prompt().user(message).call().content().strip();
    }

    @Cacheable(value = "rag-responses", key = "#query.question()")
    public String ask(String ragTemplate,AskRequest query){

        List<Document> relevantChunks = vectorStore.similaritySearch(SearchRequest.builder()
                .query(query.question())
                .topK(3).similarityThreshold(0.4)
                .build());
        var context = relevantChunks.stream().map(Document::getText).collect(Collectors.joining("\n -- \n"));
        var question = query.question();
        var template = new PromptTemplate(ragTemplate);
        var prompt = template.create(Map.of("context", context, "question", question));
        log.info("Context {}",context);
        log.info("Question {}",question);
        return chatClient.prompt().user(prompt.toString()).call().content().strip();
    }
}
