package com.benly.session.service;

import com.benly.document.entity.Document;
import com.benly.document.exception.DocumentErrorCode;
import com.benly.document.repository.DocumentRepository;
import com.benly.global.exception.BusinessException;
import com.benly.question.entity.Question;
import com.benly.question.entity.SeedQuestion;
import com.benly.question.repository.QuestionRepository;
import com.benly.question.repository.SeedQuestionRepository;
import com.benly.question.service.QuestionGenerationService;
import com.benly.session.dto.SessionCreateRequest;
import com.benly.session.dto.SessionCreateResponse;
import com.benly.session.entity.Session;
import com.benly.session.repository.SessionRepository;
import com.benly.user.entity.User;
import com.benly.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SessionService {
    private final SessionRepository sessionRepository;
    private final UserRepository userRepository; // 여기 부분은 user 담당자가 repository를 만들면 적용
    private final DocumentRepository documentRepository;
    private final QuestionGenerationService questionGenerationService;

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



        // 4. 질문생성
        questionGenerationService.generate(saved.getId(), sessionCreateRequest.jobDescription());

        // 5. 응답
        return SessionCreateResponse.from(saved.getId(), saved.getStatus());
    }

    // 소유권 검증 메서드
    private void validateDocumentOwnerShip(Long docId, Long userId) {
        Document document = documentRepository.findByIdAndDeletedAtIsNull(docId)
                .orElseThrow(() -> new BusinessException(DocumentErrorCode.DOCUMENT_NOT_FOUND));
        if (!document.getUser().getId().equals(userId)) {
            throw new BusinessException(DocumentErrorCode.DOCUMENT_FORBIDDEN);
        }
    }
}
