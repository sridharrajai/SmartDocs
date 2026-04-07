package com.sridhar.ragapi.agent;

import com.sridhar.ragapi.repository.IngestedDocumentRepository;
import com.sridhar.ragapi.util.AgentRequest;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class KnowledgeBaseTool {

    private final VectorStore vectorStore;
    private final IngestedDocumentRepository ingestedDocumentRepository;

    public KnowledgeBaseTool(VectorStore vectorStore, IngestedDocumentRepository ingestedDocumentRepository) {
        this.vectorStore = vectorStore;
        this.ingestedDocumentRepository = ingestedDocumentRepository;
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

    @Tool("Get the current date and time")
    public String getCurrentDate() {
        log.info("Tool Time Called ");
        return LocalDateTime.now()
                .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM));
    }

    @Tool("List all documents currently ingested into the knowledge base")
    public String listAvailableDocuments() {
        return ingestedDocumentRepository.findAll()
                .stream()
                .map(doc -> doc.getFilename() + " — " + doc.getChunkSize() +
                        " chunks — ingested " + doc.getIngestedAt())
                .collect(Collectors.joining("\n"));
    }

}
