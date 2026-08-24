package com.benly.feedback.service;

import com.benly.feedback.dto.FeedbackScoringResponse;
import com.benly.feedback.entity.FeedbackStatus;
import com.benly.feedback.entity.SessionFeedback;
import com.benly.feedback.event.ScoringRequestedEvent;
import com.benly.feedback.exception.FeedbackErrorCode;
import com.benly.feedback.repository.SessionFeedbackRepository;
import com.benly.feedback.repository.SessionReadRepository;
import com.benly.global.exception.BusinessException;
import com.benly.session.entity.Session;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FeedbackRequestService {

    private final SessionReadRepository sessionReadRepository;
    private final SessionFeedbackRepository sessionFeedbackRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public FeedbackScoringResponse requestScoring(Long userId, Long sessionId) {
        Session session = sessionReadRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(FeedbackErrorCode.SESSION_NOT_FOUND));
        if (!session.getUser().getId().equals(userId)) {
            throw new BusinessException(FeedbackErrorCode.SESSION_FORBIDDEN);
        }

        prepareScoring(session);

        eventPublisher.publishEvent(new ScoringRequestedEvent(sessionId));
        return FeedbackScoringResponse.from(sessionId);
    }

    private void prepareScoring(Session session) {
        switch (session.getStatus()) {
            case COMPLETED -> createScoringRecord(session);
            case ANALYZING -> resumeOrRescore(session);
            default -> throw new BusinessException(FeedbackErrorCode.SESSION_NOT_FINISHED);
        }
    }

    private void createScoringRecord(Session session) {
        session.markAnalyzing();
        insertScoringRecord(session);
    }

    private void insertScoringRecord(Session session) {
        try {
            sessionFeedbackRepository.saveAndFlush(SessionFeedback.startScoring(session));
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(FeedbackErrorCode.ALREADY_SCORED);
        }
    }

    private void resumeOrRescore(Session session) {
        Long sessionId = session.getId();
        SessionFeedback sf = sessionFeedbackRepository.findBySession_Id(sessionId).orElse(null);

        if (sf == null) {
            insertScoringRecord(session);
            return;
        }
        if (sf.isCompleted() || sf.isScoring()) {
            throw new BusinessException(FeedbackErrorCode.ALREADY_SCORED);
        }
        int updated = sessionFeedbackRepository.restartScoring(
                sessionId, FeedbackStatus.FAILED, FeedbackStatus.SCORING);
        if (updated == 0) {
            throw new BusinessException(FeedbackErrorCode.ALREADY_SCORED);
        }
    }
}