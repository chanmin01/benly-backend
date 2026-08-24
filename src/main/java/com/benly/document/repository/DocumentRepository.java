package com.benly.document.repository;

import com.benly.document.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByUserIdAndDeletedAtIsNull(Long userId);
    Optional<Document> findByIdAndDeletedAtIsNull(Long id);
}
