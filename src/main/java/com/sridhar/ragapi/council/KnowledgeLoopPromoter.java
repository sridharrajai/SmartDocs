package com.sridhar.ragapi.council;

import com.sridhar.ragapi.entity.ChatMessage;
import com.sridhar.ragapi.repository.ChatMessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class KnowledgeLoopPromoter {
    private final ChatMessageRepository chatMessageRepository;
    private final VectorStore vectorStore;

    public KnowledgeLoopPromoter(ChatMessageRepository chatMessageRepository, VectorStore vectorStore) {
        this.chatMessageRepository = chatMessageRepository;
        this.vectorStore = vectorStore;
    }


    @Scheduled(initialDelay = 10000, fixedDelay = Long.MAX_VALUE)
    public void promoteVerifiedAnswers() {
        List<ChatMessage> councilMessages=chatMessageRepository.findByCouncilVerifiedTrueAndPromotedToKnowledgeBaseFalse();
        if(councilMessages.isEmpty()){
            log.info("KnowledgeLoopPromoter: No messages to promote to Qdrant");
            return;
        }
        List<Document> promotionDocuments=new ArrayList<>();
        for(ChatMessage chatMessage:councilMessages) {
            promotionDocuments.add(new Document(chatMessage.getContent()));
            chatMessage.setPromotedToKnowledgeBase(true);
        }
        log.info("KnowledgeLoopPromoter: promoting {} messages to Qdrant", councilMessages.size());
        vectorStore.add(promotionDocuments);
        chatMessageRepository.saveAll(councilMessages);
    }
}