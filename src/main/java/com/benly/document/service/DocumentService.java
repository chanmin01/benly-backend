package com.benly.document.service;

import com.benly.document.dto.DocumentResponse;
import com.benly.document.entity.Document;
import com.benly.document.exception.DocumentErrorCode;
import com.benly.document.repository.DocumentRepository;
import com.benly.document.storage.S3StorageService;
import com.benly.global.exception.BusinessException;
import com.benly.user.entity.User;
import com.benly.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final S3StorageService s3StorageService;
    private final UserRepository userRepository;

    public List<DocumentResponse> getDocuments(Long userId) {
        List<Document> documents = documentRepository.findByUserIdAndDeletedAtIsNull(userId);

        return documents.stream()
                .map(DocumentResponse::from)
                .toList();
    }

    @Transactional
    public DocumentResponse upload(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()
                || !MediaType.APPLICATION_PDF_VALUE.equals(file.getContentType())) {
            throw new BusinessException(DocumentErrorCode.INVALID_FILE_TYPE);
        }

        String storageKey = s3StorageService.upload(file, userId);

        User user = userRepository.getReferenceById(userId);
        Document document = documentRepository.save(
                Document.create(user, file.getOriginalFilename(), storageKey));

        return DocumentResponse.from(document);
    }

    @Transactional
    public void delete(Long userId, Long docId) {
        Document document = findOwnedDocument(userId, docId);

        s3StorageService.delete(document.getStorageKey());
        document.softDelete();
    }

    @Transactional
    public DocumentResponse rename(Long userId, Long docId, String newFileName) {
        Document document = findOwnedDocument(userId, docId);

        document.rename(newFileName);
        return DocumentResponse.from(document);
    }

    private Document findOwnedDocument(Long userId, Long docId) {
        Document document = documentRepository.findByIdAndDeletedAtIsNull(docId)
                .orElseThrow(() -> new BusinessException(DocumentErrorCode.DOCUMENT_NOT_FOUND));
        if (!document.getUser().getId().equals(userId)) {
            throw new BusinessException(DocumentErrorCode.DOCUMENT_FORBIDDEN);
        }
        return document;
    }
}
