package com.benly.question.service;

import com.benly.document.entity.Document;
import com.benly.document.repository.DocumentRepository;
import com.benly.document.storage.PdfTextExtractor;
import com.benly.document.storage.S3StorageService;
import com.benly.question.client.ClaudeClient;
import com.benly.question.entity.Question;
import com.benly.question.repository.QuestionRepository;
import com.benly.question.repository.SeedQuestionRepository;
import com.benly.session.entity.Session;
import com.benly.session.repository.SessionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class QuestionGenerationServiceTest {

    @Mock private ClaudeClient claudeClient;
    @Mock private SeedQuestionRepository seedQuestionRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private SessionRepository sessionRepository;
    @Mock private DocumentRepository documentRepository;
    @Mock private S3StorageService s3StorageService;
    @Mock private PdfTextExtractor pdfTextExtractor;

    @InjectMocks
    private QuestionGenerationService questionGenerationService;

    private static final Long SESSION_ID = 1L;
    private static final Long DOC_ID = 10L;

    private Session mockSession() {
        Session session = mock(Session.class);
        given(session.getCompanyType()).willReturn("SERVICE");
        given(session.getStage()).willReturn("TECHNICAL");
        given(session.getJobTitle()).willReturn("백엔드");
        return session;
    }

    @Test
    @DisplayName("docId 있으면 서류 텍스트를 추출해 Claude에 전달")
    void generate_withDocument_passesDocText() {
        Session session = mockSession();
        given(sessionRepository.findById(SESSION_ID)).willReturn(Optional.of(session));

        // 서류 조회 → S3 다운로드 → 텍스트 추출
        Document document = mock(Document.class);
        given(document.getStorageKey()).willReturn("documents/1/abc.pdf");
        given(documentRepository.findByIdAndDeletedAtIsNull(DOC_ID))
                .willReturn(Optional.of(document));
        given(s3StorageService.download("documents/1/abc.pdf"))
                .willReturn("pdf bytes".getBytes());
        given(pdfTextExtractor.extract(any())).willReturn("자소서 내용: Spring 경험");

        given(claudeClient.generateQuestions(anyString(), anyString(), anyString(),
                any(), anyString()))
                .willReturn(List.of("질문1", "질문2", "질문3", "질문4", "질문5"));

        // when
        questionGenerationService.generate(SESSION_ID, "채용공고", DOC_ID);

        // then - Claude에 docText가 전달됐는지 캡처해서 확인
        ArgumentCaptor<String> docTextCaptor = ArgumentCaptor.forClass(String.class);
        verify(claudeClient).generateQuestions(anyString(), anyString(), anyString(),
                any(), docTextCaptor.capture());
        assertThat(docTextCaptor.getValue()).contains("자소서 내용");

        verify(session).markReady();
    }

    @Test
    @DisplayName("docId 없으면 서류 조회 없이 질문 생성 (docText=null)")
    void generate_withoutDocument_docTextNull() {
        Session session = mockSession();
        given(sessionRepository.findById(SESSION_ID)).willReturn(Optional.of(session));
        given(claudeClient.generateQuestions(anyString(), anyString(), anyString(),
                any(), isNull()))
                .willReturn(List.of("질문1", "질문2", "질문3", "질문4", "질문5"));

        // when - docId = null
        questionGenerationService.generate(SESSION_ID, "채용공고", null);

        // then - 서류 관련 호출 없음
        verify(documentRepository, never()).findByIdAndDeletedAtIsNull(any());
        verify(s3StorageService, never()).download(anyString());
        verify(claudeClient).generateQuestions(anyString(), anyString(), anyString(),
                any(), isNull());
        verify(session).markReady();
    }

    @Test
    @DisplayName("서류 추출 실패해도 질문 생성은 계속 (docText=null)")
    void generate_documentExtractFails_stillGenerates() {
        Session session = mockSession();
        given(sessionRepository.findById(SESSION_ID)).willReturn(Optional.of(session));

        // 서류 조회는 되는데 S3 다운로드 실패
        Document document = mock(Document.class);
        given(document.getStorageKey()).willReturn("documents/1/abc.pdf");
        given(documentRepository.findByIdAndDeletedAtIsNull(DOC_ID))
                .willReturn(Optional.of(document));
        given(s3StorageService.download(anyString()))
                .willThrow(new RuntimeException("S3 다운로드 실패"));

        given(claudeClient.generateQuestions(anyString(), anyString(), anyString(),
                any(), isNull()))
                .willReturn(List.of("질문1", "질문2", "질문3", "질문4", "질문5"));

        // when - 서류 실패해도
        questionGenerationService.generate(SESSION_ID, "채용공고", DOC_ID);

        // then - docText=null로 질문 생성 계속, READY
        verify(claudeClient).generateQuestions(anyString(), anyString(), anyString(),
                any(), isNull());
        verify(session).markReady();
    }

    @Test
    @DisplayName("Claude 실패 시 SeedQuestion 폴백")
    void generate_claudeFails_fallbackToSeed() {
        Session session = mockSession();
        given(sessionRepository.findById(SESSION_ID)).willReturn(Optional.of(session));

        // 서류 없음
        // Claude 실패
        given(claudeClient.generateQuestions(anyString(), anyString(), anyString(),
                any(), any()))
                .willThrow(new RuntimeException("Claude 오류"));

        // Seed 폴백
        var seed = mock(com.benly.question.entity.SeedQuestion.class);
        given(seed.getContent()).willReturn("시드 질문");
        given(seedQuestionRepository.findTop5ByCompanyTypeAndStage("SERVICE", "TECHNICAL"))
                .willReturn(List.of(seed, seed, seed, seed, seed));

        // when
        questionGenerationService.generate(SESSION_ID, "채용공고", null);

        // then - Seed로 질문 저장, READY
        verify(questionRepository, org.mockito.Mockito.times(5)).save(any(Question.class));
        verify(session).markReady();
    }
}