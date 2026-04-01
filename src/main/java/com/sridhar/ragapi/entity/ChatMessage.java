package com.sridhar.ragapi.entity;

import jakarta.persistence.*;
import jdk.jfr.Timestamp;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import javax.annotation.Nullable;
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


}
