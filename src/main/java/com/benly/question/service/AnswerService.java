package com.benly.question.service;

import com.benly.global.exception.BusinessException;
import com.benly.question.dto.AnswerCreateRequest;
import com.benly.question.dto.AnswerResponse;
import com.benly.question.dto.NextActionType;
import com.benly.question.entity.Answer;
import com.benly.question.entity.Question;
import com.benly.question.exception.AnswerErrorCode;
import com.benly.question.repository.AnswerRepository;
import com.benly.question.repository.QuestionRepository;
import com.benly.session.entity.Session;
import com.benly.session.entity.SessionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnswerService {

    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;

    private static final int MIN_ANSWER_LENGTH = 10;

    public AnswerResponse submitTextAnswer(Long sessionId, Long userId, AnswerCreateRequest request) {

        // 1. 질문조회
        Question question = questionRepository.findById(request.questionId())
                .orElseThrow(() -> new BusinessException(AnswerErrorCode.QUESTION_NOT_FOUND));

        Session session = question.getSession();

        // 2. URL의 sessionId와 질문의 세션이 일치하는지
        if (!session.getId().equals(sessionId)) {
            throw new BusinessException(AnswerErrorCode.QUESTION_SESSION_MISMATCH);
        }

        // 3. 소유권 검증
        if (!session.getUser().getId().equals(userId)){
            throw new BusinessException(AnswerErrorCode.ANSWER_FORBIDDEN);
        }

        // 4. 세션 상태 검증 (IN_PROGRESS)
        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new BusinessException(AnswerErrorCode.SESSION_NOT_IN_PROGRESS);
        }

        // 5. 중복 검증
        if (answerRepository.existsByQuestionId(question.getId())) {
            throw new BusinessException(AnswerErrorCode.ALREADY_ANSWERED);
        }

        // 6. 길이 검증
        if (request.transcript().trim().length() < MIN_ANSWER_LENGTH) {
            throw new BusinessException(AnswerErrorCode.ANSWER_TOO_SHORT);
        }

        // 7. Answer 저장
        Answer answer = Answer.createText(question, request.transcript());
        Answer saved =  answerRepository.save(answer);

        // 8. nextAction (우선은 NextMain으로 고정)
        AnswerResponse.NextAction nextAction =
                AnswerResponse.NextAction.of(NextActionType.NEXT_MAIN, null);

        return AnswerResponse.from(saved, nextAction);
    }
}
