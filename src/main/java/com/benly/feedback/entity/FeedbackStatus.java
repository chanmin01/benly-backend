package com.benly.feedback.entity;

public enum FeedbackStatus {
    SCORING("채점을 진행하고 있어요."),    // 채점 진행 중 (백그라운드 작업 도는 중)
    COMPLETED("채점이 완료되었어요."),  // 채점 완료 -> 리포트 조회 가능
    FAILED("서버 오류로 채점에 실패했어요. 다시 시도해 주세요.");      // 채점 실패 -> 재채점 허용

    private final String message;

    FeedbackStatus(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
