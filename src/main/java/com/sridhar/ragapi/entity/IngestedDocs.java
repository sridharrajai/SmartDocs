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

    public String getDoc_id() {
        return doc_id;
    }

    public void setDoc_id(String doc_id) {
        this.doc_id = doc_id;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getIngestedAt() {
        return ingestedAt;
    }

    public void setIngestedAt(Instant ingestedAt) {
        this.ingestedAt = ingestedAt;
    }
}
