package com.benly.document.exception;

import com.benly.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum DocumentErrorCode implements ErrorCode {

    DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "서류를 찾을 수 없습니다."),
    DOCUMENT_FORBIDDEN(HttpStatus.FORBIDDEN, "해당 서류에 접근할 수 없습니다.");

    private final HttpStatus status;
    private final String message;
}
