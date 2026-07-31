package com.benly.question.service;

import com.benly.question.client.ClaudeClient;
import com.benly.question.entity.Question;
import com.benly.question.entity.SeedQuestion;
import com.benly.question.repository.QuestionRepository;
import com.benly.question.repository.SeedQuestionRepository;
import com.benly.session.entity.Session;
import com.benly.session.repository.SessionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class QuestionGenerationServiceTest {

    @Mock private ClaudeClient claudeClient;
    @Mock private SeedQuestionRepository seedQuestionRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private SessionRepository sessionRepository;

    @InjectMocks
    private QuestionGenerationService questionGenerationService;

    @Test
    @DisplayName("Claude 성공 - Claude가 만든 질문 5개가 저장된다")
    void generate_claudeSuccess() {
        // given
        Long sessionId = 1L;
        Session session = mock(Session.class);
        given(session.getCompanyType()).willReturn("SERVICE");
        given(session.getStage()).willReturn("TECHNICAL");
        given(session.getJobTitle()).willReturn("백엔드");

        given(sessionRepository.findById(sessionId)).willReturn(Optional.of(session));
        given(claudeClient.generateQuestions(any(), any(), any(), any()))
                .willReturn(List.of("질문1", "질문2", "질문3", "질문4", "질문5"));

        // when
        questionGenerationService.generate(sessionId, "JD내용");

        // then
        verify(questionRepository, times(5)).save(any(Question.class));
        verify(session).markReady();
    }

    @Test
    @DisplayName("Claude 실패 - 시드로 폴백되어 저장된다")
    void generate_claudeFail_fallbackToSeed() {
        // given
        Long sessionId = 1L;
        Session session = mock(Session.class);
        given(session.getCompanyType()).willReturn("SERVICE");
        given(session.getStage()).willReturn("TECHNICAL");
        given(session.getJobTitle()).willReturn("백엔드");

        given(sessionRepository.findById(sessionId)).willReturn(Optional.of(session));
        given(claudeClient.generateQuestions(any(), any(), any(), any()))
                .willThrow(new RuntimeException("Claude 다운"));

        // seed들을 given 밖에서 먼저 만든다 (중첩 stubbing 방지)
        SeedQuestion seed1 = mockSeed("시드질문1");
        SeedQuestion seed2 = mockSeed("시드질문2");
        SeedQuestion seed3 = mockSeed("시드질문3");

        given(seedQuestionRepository.findByCompanyTypeAndStage(any(), any()))
                .willReturn(List.of(seed1, seed2, seed3));

        // when
        questionGenerationService.generate(sessionId, "JD내용");

        // then
        verify(questionRepository, times(3)).save(any(Question.class));
        verify(session).markReady();
    }

    // SeedQuestion은 생성 메서드가 없어서 Mock으로 만듦
    private SeedQuestion mockSeed(String content) {
        SeedQuestion seed = mock(SeedQuestion.class);
        given(seed.getContent()).willReturn(content);
        return seed;
    }
}