package com.sridhar.ragapi.repository;

import com.sridhar.ragapi.entity.ChatMessage;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {

    //Load Conversation
    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    //Most recent messages
    List<ChatMessage> findTop20BySessionIdOrderByCreatedAtDesc(String sessionId);
    List<ChatMessage> findByCouncilVerifiedTrueAndPromotedToKnowledgeBaseFalse();
    //Analytics
    int countBySessionId(String sessionId);

    @Modifying
    @Query(value = "UPDATE chat_messages SET council_verified = true WHERE id = (SELECT id FROM chat_messages WHERE session_id = :sessionId AND role = 'ASSISTANT' ORDER BY created_at DESC LIMIT 1)", nativeQuery = true)
    void markLatestAssistantVerified(@Param("sessionId") String sessionId);

    @Modifying
    @Transactional
    void deleteBySessionId(String string);
}
