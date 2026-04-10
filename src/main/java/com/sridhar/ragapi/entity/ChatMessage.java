package com.sridhar.ragapi.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Setter
@Getter
@Entity
@Table(name="chat_messages")
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @Column(nullable=false)
    private String sessionId;
    @Column(nullable = false)
    private String userId;
    @Enumerated(EnumType.STRING)
    private MessageRole role;
    @Column(columnDefinition="TEXT")
    private String content;
    private Integer tokenCount;
    @CreationTimestamp
    private Instant createdAt;

    @Column(columnDefinition = "boolean default false")
    private boolean councilVerified = false;

    @Column(columnDefinition = "boolean default false")
    private boolean promotedToKnowledgeBase = false;


    public ChatMessage(String sessionId, String userId, MessageRole role, String content, int tokenCount,boolean councilVerified, boolean promotedToKnowledgeBase) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.role = role;
        this.content = content;
        this.tokenCount = tokenCount;
        this.councilVerified = councilVerified;
        this.promotedToKnowledgeBase = promotedToKnowledgeBase;
    }

    public ChatMessage() {

    }

    public ChatMessage(String sessionId, String userId, MessageRole role, String content, int tokenCount) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.role = role;
        this.content = content;
        this.tokenCount = tokenCount;
    }
}
