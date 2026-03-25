package com.sridhar.ragapi.controller;

import com.sridhar.ragapi.service.ChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1")
public class ChatController {

    private final ChatService chatService;
    public ChatController( ChatService chatService){
        this.chatService = chatService;
        System.out.println("ChatController bean ready");
    }

    @PostMapping("/ask")
    public ResponseEntity<String> askQuery(@RequestBody String message){
        log.info("Received message: {}", message);
        String response = chatService.chat(message);
        log.info("Received Response: {}", response);
        return ResponseEntity.ok(response);
    }
}
