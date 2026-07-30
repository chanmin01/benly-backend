package com.benly.document.repository;

import com.benly.document.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByUserIdAndDeletedAtIsNull(Long userId);
}
