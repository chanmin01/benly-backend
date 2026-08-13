package com.benly.feedback.exception;

import com.benly.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum FeedbackErrorCode implements ErrorCode {
    SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 면접입니다."),
    SESSION_FORBIDDEN(HttpStatus.FORBIDDEN, "다른 사용자의 면접에 접근할 수 없습니다."),
    SESSION_NOT_FINISHED(HttpStatus.UNPROCESSABLE_CONTENT, "아직 종료되지 않은 면접입니다."),
    ALREADY_SCORED(HttpStatus.CONFLICT, "이미 채점이 완료된 면접입니다."),
    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "아직 결과가 생성되지 않았습니다."),
    SCORING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류로 채점에 실패했습니다. 잠시 후 히스토리에서 확인해 주세요.");

    private final HttpStatus status;
    private final String message;
}
