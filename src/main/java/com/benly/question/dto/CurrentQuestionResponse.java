package com.benly.question.dto;

import com.benly.question.entity.Question;

public record CurrentQuestionResponse(
        Long questionId,
        Integer seq,
        String content,
        String type      // MAIN / FOLLOW_UP
) {
    public static CurrentQuestionResponse from(Question question) {
        return new CurrentQuestionResponse(
                question.getId(),
                question.getSeq(),
                question.getContent(),
                question.getType()
        );
    }
}