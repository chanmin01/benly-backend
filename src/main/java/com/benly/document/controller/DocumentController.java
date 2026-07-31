package com.benly.document.controller;

import com.benly.document.dto.DocumentResponse;
import com.benly.document.service.DocumentService;
import com.benly.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @GetMapping("/documents")
    public ApiResponse<List<DocumentResponse>> getDocuments(@RequestParam Long userId) {
        List<DocumentResponse> data = documentService.getDocuments(userId);
        return ApiResponse.success("서류 목록 조회 성공", data);
    }
    // 임시 쿼리로 @RequestParam으로 받기, 인증 완성되면 추후에 변경
}
