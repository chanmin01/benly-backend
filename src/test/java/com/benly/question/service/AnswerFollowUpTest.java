package com.benly.question.service;

import com.benly.question.client.ClaudeClient;
import com.benly.question.client.WhisperClient;
import com.benly.question.dto.AnswerCreateRequest;
import com.benly.question.dto.AnswerResponse;
import com.benly.question.entity.Answer;
import com.benly.question.entity.NextActionType;
import com.benly.question.entity.Question;
import com.benly.question.repository.AnswerRepository;
import com.benly.question.repository.QuestionRepository;
import com.benly.session.entity.Session;
import com.benly.session.entity.SessionStatus;
import com.benly.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // 불필요한 스터빙 에러(UnnecessaryStubbing) 방지 추가
class AnswerFollowUpTest {

    @Mock private AnswerRepository answerRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private ClaudeClient claudeClient;
    @Mock private WhisperClient whisperClient;

    @InjectMocks
    private AnswerService answerService;

    private static final Long USER_ID = 1L;
    private static final Long SESSION_ID = 10L;

    private User mockUser() {
        User user = mock(User.class);
        given(user.getId()).willReturn(USER_ID);
        return user;
    }

    private Session mockSession() {
        Session session = mock(Session.class);

        User user = mockUser();

        given(session.getId()).willReturn(SESSION_ID);
        given(session.getUser()).willReturn(user);
        given(session.getStatus()).willReturn(SessionStatus.IN_PROGRESS);
        return session;
    }

    private void stubCommonChecks(Question question) {
        given(questionRepository.findById(question.getId()))
                .willReturn(Optional.of(question));
        given(answerRepository.existsByQuestionId(question.getId())).willReturn(false);
        given(answerRepository.saveAndFlush(any(Answer.class)))
                .willAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("메인 질문에 답변하면 꼬리1 생성 → FOLLOW_UP")
    void mainAnswer_createsFollowUp() {
        // given
        Session session = mockSession();

        Question mainQuestion = mock(Question.class);
        given(mainQuestion.getId()).willReturn(100L);
        given(mainQuestion.getParent()).willReturn(null);
        given(mainQuestion.getContent()).willReturn("메인 질문입니다");
        given(mainQuestion.getSession()).willReturn(session);

        stubCommonChecks(mainQuestion);

        given(claudeClient.generateFollowUp(anyString())).willReturn("꼬리질문입니다");

        Question savedFollowUp = mock(Question.class);
        given(savedFollowUp.getId()).willReturn(200L);
        given(questionRepository.save(any(Question.class))).willReturn(savedFollowUp);

        given(answerRepository.findByQuestionId(100L)).willReturn(Optional.empty());
        given(questionRepository.findByParentOrderBySeqAsc(mainQuestion))
                .willReturn(List.of());

        AnswerCreateRequest request = new AnswerCreateRequest(100L, "메인 질문에 대한 충분한 답변입니다");

        // when
        AnswerResponse response = answerService.submitTextAnswer(SESSION_ID, USER_ID, request);

        // then
        assertThat(response.nextAction().type()).isEqualTo(NextActionType.FOLLOW_UP);
        assertThat(response.nextAction().nextQuestionId()).isEqualTo(200L);
    }

    @Test
    @DisplayName("꼬리1에 답변하면 꼬리2 생성 → FOLLOW_UP")
    void followUp1Answer_createsFollowUp2() {
        // given
        Session session = mockSession();

        Question mainQuestion = mock(Question.class);
        given(mainQuestion.getId()).willReturn(100L);
        given(mainQuestion.getContent()).willReturn("메인 질문");

        Question followUp1 = mock(Question.class);
        given(followUp1.getId()).willReturn(200L);
        given(followUp1.getParent()).willReturn(mainQuestion);
        given(followUp1.getSession()).willReturn(session);

        stubCommonChecks(followUp1);

        given(questionRepository.countByParent(mainQuestion)).willReturn(1);
        given(claudeClient.generateFollowUp(anyString())).willReturn("꼬리질문2");

        Question savedFollowUp2 = mock(Question.class);
        given(savedFollowUp2.getId()).willReturn(300L);
        given(questionRepository.save(any(Question.class))).willReturn(savedFollowUp2);

        given(answerRepository.findByQuestionId(any())).willReturn(Optional.empty());
        given(questionRepository.findByParentOrderBySeqAsc(mainQuestion)).willReturn(List.of());

        AnswerCreateRequest request = new AnswerCreateRequest(200L, "꼬리1에 대한 답변입니다");

        // when
        AnswerResponse response = answerService.submitTextAnswer(SESSION_ID, USER_ID, request);

        // then
        assertThat(response.nextAction().type()).isEqualTo(NextActionType.FOLLOW_UP);
        assertThat(response.nextAction().nextQuestionId()).isEqualTo(300L);
    }

    @Test
    @DisplayName("꼬리2에 답변하면 다음 메인 → NEXT_MAIN")
    void followUp2Answer_nextMain() {
        // given
        Session session = mockSession();

        Question mainQuestion = mock(Question.class);
        given(mainQuestion.getId()).willReturn(100L);
        given(mainQuestion.getSeq()).willReturn(1);

        Question followUp2 = mock(Question.class);
        given(followUp2.getId()).willReturn(300L);
        given(followUp2.getParent()).willReturn(mainQuestion);
        given(followUp2.getSession()).willReturn(session);

        stubCommonChecks(followUp2);

        given(questionRepository.countByParent(mainQuestion)).willReturn(2);

        Question nextMain = mock(Question.class);
        given(nextMain.getId()).willReturn(101L);
        given(questionRepository.findFirstBySessionAndParentIsNullAndSeqGreaterThanOrderBySeqAsc(
                session, 1)).willReturn(Optional.of(nextMain));

        AnswerCreateRequest request = new AnswerCreateRequest(300L, "꼬리2에 대한 답변입니다");

        // when
        AnswerResponse response = answerService.submitTextAnswer(SESSION_ID, USER_ID, request);

        // then
        assertThat(response.nextAction().type()).isEqualTo(NextActionType.NEXT_MAIN);
        assertThat(response.nextAction().nextQuestionId()).isEqualTo(101L);
    }

    @Test
    @DisplayName("마지막 메인의 꼬리2에 답변하면 → FINISH")
    void lastFollowUp2Answer_finish() {
        // given
        Session session = mockSession();

        Question mainQuestion = mock(Question.class);
        given(mainQuestion.getId()).willReturn(100L);
        given(mainQuestion.getSeq()).willReturn(5);

        Question followUp2 = mock(Question.class);
        given(followUp2.getId()).willReturn(300L);
        given(followUp2.getParent()).willReturn(mainQuestion);
        given(followUp2.getSession()).willReturn(session);

        stubCommonChecks(followUp2);

        given(questionRepository.countByParent(mainQuestion)).willReturn(2);

        given(questionRepository.findFirstBySessionAndParentIsNullAndSeqGreaterThanOrderBySeqAsc(
                session, 5)).willReturn(Optional.empty());

        // BusinessException 방지를 위해 문자열 길이를 늘렸습니다.
        AnswerCreateRequest request = new AnswerCreateRequest(300L, "마지막 꼬리질문에 대한 충분한 길이의 답변입니다.");

        // when
        AnswerResponse response = answerService.submitTextAnswer(SESSION_ID, USER_ID, request);

        // then
        assertThat(response.nextAction().type()).isEqualTo(NextActionType.FINISH);
        assertThat(response.nextAction().nextQuestionId()).isNull();
    }

    @Test
    @DisplayName("꼬리 생성 실패해도 답변 저장 + 다음 메인으로")
    void followUpFails_stillProceeds() {
        // given
        Session session = mockSession();

        Question mainQuestion = mock(Question.class);
        given(mainQuestion.getId()).willReturn(100L);
        given(mainQuestion.getSeq()).willReturn(1);
        given(mainQuestion.getContent()).willReturn("메인 질문");
        given(mainQuestion.getParent()).willReturn(null);
        given(mainQuestion.getSession()).willReturn(session);

        stubCommonChecks(mainQuestion);

        given(claudeClient.generateFollowUp(anyString()))
                .willThrow(new RuntimeException("Claude 오류"));

        given(answerRepository.findByQuestionId(any())).willReturn(Optional.empty());
        given(questionRepository.findByParentOrderBySeqAsc(mainQuestion)).willReturn(List.of());

        Question nextMain = mock(Question.class);
        given(nextMain.getId()).willReturn(101L);
        given(questionRepository.findFirstBySessionAndParentIsNullAndSeqGreaterThanOrderBySeqAsc(
                session, 1)).willReturn(Optional.of(nextMain));

        AnswerCreateRequest request = new AnswerCreateRequest(100L, "메인 질문에 대한 답변입니다");

        // when
        AnswerResponse response = answerService.submitTextAnswer(SESSION_ID, USER_ID, request);

        // then
        assertThat(response.nextAction().type()).isEqualTo(NextActionType.NEXT_MAIN);
        assertThat(response.answer()).isNotNull();
    }

    @Test
    @DisplayName("음성 답변 → Whisper 변환 후 저장 (AUDIO)")
    void audioAnswer_transcribesAndSaves() {
        Session session = mockSession();

        Question main = mock(Question.class);
        given(main.getId()).willReturn(100L);
        given(main.getParent()).willReturn(null);
        given(main.getContent()).willReturn("메인 질문");
        given(main.getSession()).willReturn(session);

        stubCommonChecks(main);

        // Whisper가 음성 → 텍스트 변환
        given(whisperClient.transcribe(any(MultipartFile.class)))
                .willReturn("음성에서 변환된 충분한 길이의 답변입니다");

        given(claudeClient.generateFollowUp(anyString())).willReturn("꼬리질문");
        Question savedFollowUp = mock(Question.class);
        given(savedFollowUp.getId()).willReturn(200L);
        given(questionRepository.save(any(Question.class))).willReturn(savedFollowUp);
        given(answerRepository.findByQuestionId(any())).willReturn(Optional.empty());
        given(questionRepository.findByParentOrderBySeqAsc(main)).willReturn(List.of());

        MultipartFile audioFile = new MockMultipartFile(
                "audio", "answer.m4a", "audio/m4a", "dummy audio bytes".getBytes());

        // when
        AnswerResponse response = answerService.submitAudioAnswer(
                SESSION_ID, USER_ID, 100L, audioFile);

        // then
        assertThat(response.answer().inputType()).isEqualTo("AUDIO");
        assertThat(response.answer().transcript()).contains("변환된");
        assertThat(response.answer().sttStatus()).isEqualTo("COMPLETED");
    }
}