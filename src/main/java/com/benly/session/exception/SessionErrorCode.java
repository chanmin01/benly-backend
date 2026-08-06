package com.benly.session.exception;

import com.benly.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SessionErrorCode implements ErrorCode {
    SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "세션을 찾을 수 없습니다."),
    SESSION_FORBIDDEN(HttpStatus.FORBIDDEN, "해당 세션에 접근할 수 없습니다."),
    SESSION_NOT_READY(HttpStatus.CONFLICT, "시작할 수 없는 세션 상태입니다."),
    QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "질문을 찾을 수 없습니다."),
    SESSION_NOT_COMPLETED(HttpStatus.CONFLICT, "완료된 면접이 아닙니다.");

    private final HttpStatus status;
    private final String message;
}
