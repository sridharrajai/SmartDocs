package com.sridhar.ragapi.controller;

import com.sridhar.ragapi.entity.IngestedDocs;
import com.sridhar.ragapi.service.ChatService;
import com.sridhar.ragapi.service.IngestService;
import com.sridhar.ragapi.util.AskRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1")
public class ChatController {

    private final ChatService chatService;
    private final IngestService ingestService;


    public ChatController(ChatService chatService, IngestService ingestService) {
        this.chatService = chatService;
        this.ingestService = ingestService;
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

    @PostMapping("/ingest")
    public ResponseEntity<String> ingestDoc(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("No file uploaded");
        }
        log.info("Received file: {}", file.getOriginalFilename());
        int chunkCount = ingestService.ingest(file);
        return ResponseEntity.ok("Ingested " + chunkCount + " chunks");


    }

    @GetMapping("/documents")
    public ResponseEntity<String> postIngestion()
    {
        List<IngestedDocs> allDocs = ingestService.getAllChunks();
        return ResponseEntity.ok(allDocs.toString());

    }

}
