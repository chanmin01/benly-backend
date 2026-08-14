package com.benly.feedback.service;

import com.benly.feedback.dto.FeedbackScoringResponse;
import com.benly.feedback.entity.SessionFeedback;
import com.benly.feedback.exception.FeedbackErrorCode;
import com.benly.feedback.repository.SessionFeedbackRepository;
import com.benly.feedback.repository.SessionReadRepository;
import com.benly.global.exception.BusinessException;
import com.benly.session.entity.Session;
import com.benly.session.entity.SessionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class FeedbackRequestService {

    private final SessionReadRepository sessionReadRepository;
    private final SessionFeedbackRepository sessionFeedbackRepository;
    private final FeedbackCommandService feedbackCommandService;

    @Transactional
    public FeedbackScoringResponse requestScoring(Long userId, Long sessionId) {
        Session session = sessionReadRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(FeedbackErrorCode.SESSION_NOT_FOUND));

        if (!session.getUser().getId().equals(userId)) {
            throw new BusinessException(FeedbackErrorCode.SESSION_FORBIDDEN);
        }

        startScoring(session);                     // 상태 검증 + SCORING 기록 준비
        feedbackCommandService.score(sessionId);   // 커밋 후 백그라운드 채점 시작
        return FeedbackScoringResponse.from(sessionId);
    }

    /** 세션 상태에 따라 채점을 시작할 수 있는지 판단하고 SCORING 기록을 준비한다. */
    private void startScoring(Session session) {
        SessionStatus status = session.getStatus();

        if (status == SessionStatus.COMPLETED) {
            session.markAnalyzing();
            sessionFeedbackRepository.save(SessionFeedback.startScoring(session));
            return;
        }
        if (status == SessionStatus.ANALYZING) {
            SessionFeedback sf = sessionFeedbackRepository.findBySession_Id(session.getId()).orElse(null);
            if (sf == null) {
                sessionFeedbackRepository.save(SessionFeedback.startScoring(session));
            } else if (sf.isCompleted() || sf.isScoring()) {
                throw new BusinessException(FeedbackErrorCode.ALREADY_SCORED);
            } else {
                sf.resetForRescore();   // 직전 실패(FAILED) → 다시 SCORING
            }
            return;
        }
        throw new BusinessException(FeedbackErrorCode.SESSION_NOT_FINISHED);
    }
}
