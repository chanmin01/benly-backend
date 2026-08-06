package com.benly.session.service;

import com.benly.document.entity.Document;
import com.benly.document.exception.DocumentErrorCode;
import com.benly.document.repository.DocumentRepository;
import com.benly.global.exception.BusinessException;
import com.benly.question.entity.Question;
import com.benly.question.repository.QuestionRepository;
import com.benly.question.service.QuestionGenerationService;
import com.benly.session.dto.*;
import com.benly.session.entity.Session;
import com.benly.session.entity.SessionStatus;
import com.benly.session.exception.SessionErrorCode;
import com.benly.session.repository.SessionRepository;
import com.benly.user.entity.User;
import com.benly.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SessionService {
    private final SessionRepository sessionRepository;
    private final UserRepository userRepository; // 여기 부분은 user 담당자가 repository를 만들면 적용
    private final DocumentRepository documentRepository;
    private final QuestionRepository questionRepository;

    @Transactional
    public SessionCreateResponse createSession(Long userId, SessionCreateRequest sessionCreateRequest) {
        // 1. 유저 조회
        User user = userRepository.findById(userId) // 여기도 만든 메스뎅 따라서 추후에 변경 예정
                .orElseThrow(() -> new IllegalArgumentException("유저 없음")); // 나중에 예외 정리

        // 2.docId 있으면 소유권 검증
        if (sessionCreateRequest.docId() != null) {
            validateDocumentOwnerShip(sessionCreateRequest.docId(),userId);
        }
        // 3. Session 생성 + 저장
        Session session = Session.create(
                user,
                sessionCreateRequest.companyType(),
                sessionCreateRequest.interviewStage(),
                sessionCreateRequest.jobRole(),
                sessionCreateRequest.companyName()
        );
        Session saved =  sessionRepository.save(session);


        // 5. 응답
        return SessionCreateResponse.from(saved.getId(), saved.getStatus().name());
    }

    // 소유권 검증 메서드
    private void validateDocumentOwnerShip(Long docId, Long userId) {
        Document document = documentRepository.findByIdAndDeletedAtIsNull(docId)
                .orElseThrow(() -> new BusinessException(DocumentErrorCode.DOCUMENT_NOT_FOUND));
        if (!document.getUser().getId().equals(userId)) {
            throw new BusinessException(DocumentErrorCode.DOCUMENT_FORBIDDEN);
        }
    }

    @Transactional
    public SessionStartResponse startSession(Long userId, Long sessionId) {
        // 1. 세션 조회
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(SessionErrorCode.SESSION_NOT_FOUND));

        // 2. 소유권 검증
        if (!session.getUser().getId().equals(userId)){
            throw new BusinessException(SessionErrorCode.SESSION_FORBIDDEN);
        }

        // 3. 원자적 상태 전환 (READY → IN_PROGRESS) - 동시 요청 방어
        int updated = sessionRepository.updateStatusIfCurrent(
                sessionId, SessionStatus.READY, SessionStatus.IN_PROGRESS);
        if (updated == 0) {
            // 0 = READY가 아니었음 (이미 시작됐거나 생성 중)
            throw new BusinessException(SessionErrorCode.SESSION_NOT_READY);
        }

        // 4. 세션 상태를 IN_PROGRESS로
        session.markInProgress();

        // 5. 첫 질문 조회
        Question firstQuestion = questionRepository
                .findFirstBySessionAndParentIsNullOrderBySeqAsc(session)
                .orElseThrow(() -> new BusinessException(SessionErrorCode.QUESTION_NOT_FOUND));

        // 6. 응답
        return SessionStartResponse.from(session, firstQuestion);
    }

    // SessionService에 추가

    // 세션 상세 조회
    @Transactional(readOnly = true)
    public SessionDetailResponse getSession(Long userId, Long sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(SessionErrorCode.SESSION_NOT_FOUND));

        if (!session.getUser().getId().equals(userId)) {
            throw new BusinessException(SessionErrorCode.SESSION_FORBIDDEN);
        }

        // 진행도 계산 (메인 기준)
        int total = questionRepository.countBySessionAndParentIsNull(session);
        int unanswered = questionRepository.findUnansweredMains(sessionId).size();
        int current = total - unanswered;   // 답변한 메인 수

        return SessionDetailResponse.from(session, current, total);
    }

    // 질문 생성 상태 폴링
    @Transactional(readOnly = true)
    public GenerationStatusResponse getGenerationStatus(Long userId, Long sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(SessionErrorCode.SESSION_NOT_FOUND));

        if (!session.getUser().getId().equals(userId)) {
            throw new BusinessException(SessionErrorCode.SESSION_FORBIDDEN);
        }

        return GenerationStatusResponse.from(session);
    }

    // 채점 요청 (analyze)
    @Transactional
    public AnalyzeResponse analyze(Long userId, Long sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(SessionErrorCode.SESSION_NOT_FOUND));

        if (!session.getUser().getId().equals(userId)) {
            throw new BusinessException(SessionErrorCode.SESSION_FORBIDDEN);
        }

        // COMPLETED만 채점 요청 가능
        if (session.getStatus() != SessionStatus.COMPLETED) {
            throw new BusinessException(SessionErrorCode.SESSION_NOT_COMPLETED);
        }

        session.markAnalyzing();   // ANALYZING

        return AnalyzeResponse.from(session);
    }


}
