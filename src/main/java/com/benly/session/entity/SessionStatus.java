package com.benly.session.entity;

public enum SessionStatus {
    GENERATING,   // 질문 생성 중
    READY,        // 준비됨
    IN_PROGRESS,  // 진행 중
    FAILED,       // 실패
    COMPLETED     // 완료
}
