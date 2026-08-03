package com.benly.session.dto;

import com.benly.question.entity.Question;
import com.benly.session.entity.Session;

public record SessionStartResponse(
        Long sessionId,
        String status,
        FirstQuestion firstQuestion
) {
    public record FirstQuestion(
            Long questionId,
            Integer seq,
            String content
    ) {
        public static FirstQuestion from(Question question) {
            return new FirstQuestion(
                    question.getId(),
                    question.getSeq(),
                    question.getContent()
            );
        }
    }

    public static SessionStartResponse from(Session session, Question firstQuestion) {
        return new SessionStartResponse(
                session.getId(),
                session.getStatus().name(),
                FirstQuestion.from(firstQuestion)
        );
    }
}