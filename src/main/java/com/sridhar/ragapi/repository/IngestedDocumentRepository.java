package com.sridhar.ragapi.repository;

import com.sridhar.ragapi.entity.IngestedDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface IngestedDocumentRepository extends JpaRepository<IngestedDocument, String> {
    List<IngestedDocument> findByStatus(String status);
    long countByIngestedAtAfter(Instant since);
}
