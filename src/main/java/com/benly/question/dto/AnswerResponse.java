package com.benly.question.dto;

import com.benly.question.entity.Answer;

public record AnswerResponse(
        AnswerInfo answer,
        NextAction nextAction
) {
    public record AnswerInfo(
            Long answerId,
            Long questionId,
            String inputType,
            String transcript,
            Integer durationSeconds,
            String sttStatus
    ){
        public static AnswerInfo from(Answer answer) {
            return new AnswerInfo(
                    answer.getId(),
                    answer.getQuestion().getId(),
                    answer.getInputType().name(),
                    answer.getTranscript(),
                    answer.getDurationSec(),
                    answer.getSttStatus()
            );
        }
    }

    public record NextAction(
            NextActionType type,
            Long nextQuestionId,
            String message
    ){
        public static NextAction of(NextActionType type, Long nextQuestionId) {
            return new NextAction(type, nextQuestionId, type.getDefaultMessage());
        }
    }

    public static AnswerResponse from(Answer answer, NextAction nextAction) {
        return new AnswerResponse(AnswerInfo.from(answer), nextAction);
    }
}
