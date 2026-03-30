package com.sridhar.ragapi.entity;

import jakarta.persistence.*;
import jdk.jfr.Timestamp;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "ingested_docs")
public class IngestedDocs {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String doc_id;

    @Column(nullable = false)
    private String filename;

    private String modelName;
    private int chunkSize;
    private String status;
    @CreationTimestamp
    @Column(columnDefinition = "timestamp with time zone")
    private Instant ingestedAt;

    public static IngestedDocs of(String filename,
                                      int chunkCount,
                                      String embeddingModel) {
        IngestedDocs doc = new IngestedDocs();
        doc.filename = filename;
        doc.chunkSize = chunkCount;
        doc.modelName = embeddingModel;
        doc.status = "COMPLETED";
        return doc;
    }

    
}
