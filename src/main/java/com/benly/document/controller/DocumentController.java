package com.benly.document.controller;

import com.benly.document.dto.DocumentRenameRequest;
import com.benly.document.dto.DocumentResponse;
import com.benly.document.service.DocumentService;
import com.benly.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @GetMapping
    public ApiResponse<List<DocumentResponse>> getDocuments(@AuthenticationPrincipal Long userId) {
        List<DocumentResponse> data = documentService.getDocuments(userId);
        return ApiResponse.success("서류 목록 조회 성공", data);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<DocumentResponse> upload(
            @AuthenticationPrincipal Long userId,
            @RequestParam("file") MultipartFile file) {
        return ApiResponse.success("서류가 업로드되었습니다.", documentService.upload(userId, file));
    }

    @DeleteMapping("/{docId}")
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long docId
    ) {
        documentService.delete(userId, docId);
        return ApiResponse.success("서류가 삭제되었습니다.");
    }

    @PatchMapping("/{docId}")
    public ApiResponse<DocumentResponse> rename(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long docId,
            @Valid @RequestBody DocumentRenameRequest request) {
        return ApiResponse.success("서류 이름이 수정되었습니다.",
                documentService.rename(userId, docId, request.fileName()));
    }
}
