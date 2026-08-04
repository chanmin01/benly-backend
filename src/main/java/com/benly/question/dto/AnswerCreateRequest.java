package com.benly.question.dto;

import jakarta.validation.constraints.NotNull;

public record AnswerCreateRequest(
        @NotNull(message = "질문 ID는 필수입니다.")
        Long questionId,
        @NotNull(message = "답변 내용은 필수입니다.")
        String transcript
) {
}
