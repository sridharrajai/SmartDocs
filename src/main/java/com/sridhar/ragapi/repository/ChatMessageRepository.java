package com.sridhar.ragapi.repository;

import com.sridhar.ragapi.entity.ChatMessage;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {

    //Load Conversation
    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    //Most recent messages
    List<ChatMessage> findTop20BySessionIdOrderByCreatedAtDesc(String sessionId);

    //Analytics
    int countBySessionId(String sessionId);

    @Modifying
    @Transactional
    void deleteBySessionId(String string);
}
