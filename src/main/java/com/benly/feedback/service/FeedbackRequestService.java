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

        SessionStatus status = session.getStatus();

        if (status == SessionStatus.COMPLETED) {
            session.markAnalyzing();
        } else if (status == SessionStatus.ANALYZING) {
            SessionFeedback sf = sessionFeedbackRepository.findBySession_Id(sessionId).orElse(null);
            if (sf != null && (sf.isCompleted() || sf.isScoring())) {
                throw new BusinessException(FeedbackErrorCode.ALREADY_SCORED);
            }
        } else {
            throw new BusinessException(FeedbackErrorCode.SESSION_NOT_FINISHED);
        }

        feedbackCommandService.score(sessionId);

        return FeedbackScoringResponse.from(sessionId);
    }
}
