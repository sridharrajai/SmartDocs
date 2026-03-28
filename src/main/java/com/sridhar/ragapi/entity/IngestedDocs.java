package com.sridhar.ragapi.entity;

import jakarta.persistence.*;
import jdk.jfr.Timestamp;

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
    @Timestamp
    private Instant ingestedAt;
}
