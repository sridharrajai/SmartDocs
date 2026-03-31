package com.sridhar.ragapi.repository;

import com.sridhar.ragapi.entity.IngestedDocs;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface IngestedDocumentRepository extends JpaRepository<IngestedDocs, String> {
    List<IngestedDocs> findByStatus(String status);
    long countByIngestedAtAfter(Instant since);
}
