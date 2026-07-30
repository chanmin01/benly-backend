package com.benly.document.dto;

import com.benly.document.entity.Document;

import java.time.LocalDateTime;

public record DocumentResponse (
        Long docId,
        String fileName,
        LocalDateTime createdAt
){
    public static DocumentResponse from(Document document) {
        return new DocumentResponse(document.getId(), document.getFileName(), document.getCreatedAt());
    }
}
