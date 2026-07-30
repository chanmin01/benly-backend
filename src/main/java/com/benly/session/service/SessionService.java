package com.benly.session.service;

import com.benly.document.entity.Document;
import com.benly.document.exception.DocumentErrorCode;
import com.benly.global.exception.BusinessException;
import com.benly.session.dto.SessionCreateRequest;
import com.benly.session.dto.SessionCreateResponse;
import com.benly.session.entity.Session;
import com.benly.session.repository.SessionRepository;
import com.benly.user.entity.User;
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

        // 4. TODO: 비동기로 질문 생성 시작

        // 5. 응답
        return SessionCreateResponse.from(saved.getId(), saved.getStatus());
    }


    // 소유권 검증 메서드
    private void validateDocumentOwnerShip(Long docId, Long userId) {
        Document document = documentRepository.findById(docId)
                .orElseThrow(() -> new BusinessException(DocumentErrorCode.DOCUMENT_NOT_FOUND));
        if (!document.getUser().getId().equals(userId)) {
            throw new BusinessException(DocumentErrorCode.DOCUMENT_FORBIDDEN);
        }
    }
}
