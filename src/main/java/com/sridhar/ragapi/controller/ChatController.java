package com.sridhar.ragapi.controller;

import com.sridhar.ragapi.service.ChatService;
import com.sridhar.ragapi.util.AskRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/v1")
public class ChatController {

    private final ChatService chatService;


    public ChatController(ChatService chatService) {
        this.chatService = chatService;
        System.out.println("ChatController bean ready");

    }

    @PostMapping("/chat")
    public ResponseEntity<String> chat(@RequestBody String message){
        log.info("Received message: {}", message);
        String response = chatService.chat(message);
        log.info("Received Response: {}", response);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/ask")
    public ResponseEntity<String> askQuery(@RequestBody @Valid AskRequest query){

        // Prompt template using Java 21 text block
        // Note the {context} and {question} placeholders — these get filled at runtime
        var ragTemplate = """    
        You are a document analysis assistant.
        Answer ONLY from the context provided below.
                If the answer is not in the context, say: 'I don't have that information.'
        Do not add information from your general knowledge.
        Context:
        {context}
        Question: {question}
        """;

        var response = chatService.ask(ragTemplate,query);
        return ResponseEntity.ok(response);
    }

}
