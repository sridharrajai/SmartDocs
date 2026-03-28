package com.sridhar.ragapi.service;

import com.sridhar.ragapi.entity.IngestedDocs;
import com.sridhar.ragapi.repository.IngestedDocumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
@Slf4j
public class IngestService {


    private final QdrantVectorStore vectorStore;
    private final IngestedDocumentRepository ingestedDocumentRepository;

    public IngestService(QdrantVectorStore vectorStore, IngestedDocumentRepository ingestedDocumentRepository) {
        this.vectorStore = vectorStore;
        this.ingestedDocumentRepository = ingestedDocumentRepository;
    }

    public int ingest(MultipartFile file) throws IOException {
        // Save to temp file — PagePdfDocumentReader needs a Resource
        Path temp = Files.createTempFile("upload-", ".pdf");
        file.transferTo(temp);
        try {
            // Read the PDF — extracts text page by page
            PagePdfDocumentReader reader = new PagePdfDocumentReader(
                    new FileSystemResource(temp)
            );

            if (reader.get() == null || reader.get().isEmpty()) {
                throw new IllegalArgumentException("Failed to extract text from PDF. Please ensure the file is a valid PDF and contains extractable text.");
            }
            // Split into chunks — 512 tokens, 50 token overlap
            TokenTextSplitter splitter = new TokenTextSplitter(512, 5, 50, 10000, true, List.of(',', '.'));
            List<Document> chunks = splitter.apply(reader.get());
            if (chunks.isEmpty()) {
                throw new IllegalArgumentException("Failed to split text into chunks. Please ensure the PDF contains sufficient text for splitting.");
            }
            vectorStore.add(chunks);
            //ingestedDocumentRepository.save(new IngestedDocs(file.getOriginalFilename(), "gpt-3.5-turbo", chunks.size(), "ingested"));
            return chunks.size();
        } catch(IllegalArgumentException e){
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to process PDF: " + e.getMessage(),e);
        }

        finally {
            Files.deleteIfExists(temp);
        }

    }

    public List<IngestedDocs> getAllChunks(){
       List<IngestedDocs> docs = ingestedDocumentRepository.findAll();
       log.info("Retrieved {} ingested documents from the database", docs);
       return docs;
    }
}
