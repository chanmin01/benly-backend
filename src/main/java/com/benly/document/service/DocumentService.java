package com.benly.document.service;

import com.benly.document.dto.DocumentResponse;
import com.benly.document.entity.Document;
import com.benly.document.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DocumentService {

    private final DocumentRepository documentRepository;

    public List<DocumentResponse> getDocuments(Long userId) {
        List<Document> documents = documentRepository.findByUserIdAndDeletedAtIsNull(userId);

        return documents.stream()
                .map(DocumentResponse::from)
                .toList();
    }
}
