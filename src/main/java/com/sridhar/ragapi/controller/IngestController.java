package com.sridhar.ragapi.controller;

import com.sridhar.ragapi.entity.IngestedDocument;
import com.sridhar.ragapi.service.IngestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1")
public class IngestController {
    private final IngestService ingestService;

    public IngestController(IngestService ingestService) {
        this.ingestService = ingestService;
    }

    @PostMapping("/ingest")
    public ResponseEntity<String> ingestDoc(@RequestParam("file") @NotNull MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("No file uploaded");
        }
        log.info("Received file: {}", file.getOriginalFilename());
        int chunkCount = ingestService.ingest(file);
        return ResponseEntity.ok("Ingested " + chunkCount + " chunks");


    }

    @GetMapping("/documents")
    public ResponseEntity<List<IngestedDocument>> getAllDocuments() {
        return ResponseEntity.ok(ingestService.getAllChunks());
    }
}
