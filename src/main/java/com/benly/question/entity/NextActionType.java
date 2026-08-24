package com.benly.question.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NextActionType {

    FOLLOW_UP("답변을 바탕으로 추가 질문을 준비했어요."),
    NEXT_MAIN("다음 질문으로 넘어갈게요."),
    FINISH("면접이 종료되었어요, 수고하셨어요!"),
    RETRY_INPUT("다시 입력해주세요.");

    private final String defaultMessage;
}


