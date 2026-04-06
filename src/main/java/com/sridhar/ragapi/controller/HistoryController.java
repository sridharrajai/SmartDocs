package com.sridhar.ragapi.controller;

import com.sridhar.ragapi.entity.ChatMessage;
import com.sridhar.ragapi.repository.ChatMessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/chat")
public class HistoryController {

    private final ChatMessageRepository chatMessageRepository;

    public HistoryController(ChatMessageRepository chatMessageRepository) {
        this.chatMessageRepository = chatMessageRepository;
    }

    @GetMapping("/history")
    public ResponseEntity<List<ChatMessage>> getChatHistory(@RequestParam String sessionId) {
        log.info("Fetching chat history for sessionId: {}", sessionId);
        List<ChatMessage> history = chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        return ResponseEntity.ok(history);
    }
}
