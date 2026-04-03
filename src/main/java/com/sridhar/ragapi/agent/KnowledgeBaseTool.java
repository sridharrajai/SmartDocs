package com.sridhar.ragapi.agent;

import com.sridhar.ragapi.util.AgentRequest;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class KnowledgeBaseTool {

    private final VectorStore vectorStore;


    public KnowledgeBaseTool(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Tool("Search the knowledge base ONLY when the user asks about uploaded documents, " +
            "PDF content, or information that was previously ingested into the system. " +
            "Do NOT call this for general knowledge questions, math, or common facts.")
    public String knowledgeBase(AgentRequest request) {
        List<Document> retrievedDocs = vectorStore.similaritySearch(SearchRequest.builder()
                .query(request.userQuery())
                .topK(3).similarityThreshold(0.4)
                .build());
        var joinedDocs = retrievedDocs.stream().map(Document :: getText).collect(Collectors.joining("\n -- \n"));
        AgentContextHolder.setContext(joinedDocs);
        log.info("Tool Called ");
        return joinedDocs;
    }
}
