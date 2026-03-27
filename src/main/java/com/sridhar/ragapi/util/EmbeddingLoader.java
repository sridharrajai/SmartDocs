package com.sridhar.ragapi.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class EmbeddingLoader implements CommandLineRunner {
    private final VectorStore vectorStore;
    public EmbeddingLoader(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }
    @Override
    public void run(String... args) {
        var docs = List.of(
                new Document("Java 21 introduced virtual threads — lightweight threads managed by the JVM."),
                new Document("RAG stands for Retrieval Augmented Generation. It grounds LLM answers in documents."),
                new Document("Spring AI provides a unified API for multiple LLM providers including OpenAI and Gemini.")
        );
        vectorStore.add(docs);
        log.info("Stored {} documents in Qdrant", docs.size());
    }
}
