package com.benly.feedback.dto;

import com.benly.feedback.dto.ScoringResult.MainQuestionInput;
import com.benly.feedback.dto.ScoringResult.MainQuestionScore;
import com.benly.feedback.dto.ScoringResult.TailInput;
import com.benly.feedback.entity.Axis;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ScoringResultTest {

    private MainQuestionInput input(String mainAnswer, String... tailAnswers) {
        List<TailInput> tails = Arrays.stream(tailAnswers)
                .map(a -> new TailInput(null, "꼬리질문", a))
                .toList();
        return new MainQuestionInput(1, "메인질문", mainAnswer, tails);
    }

    @Test
    @DisplayName("모든 질문에 답하면 완성도 1.0, 건너뜀 0")
    void allAnswered() {
        // given
        MainQuestionInput in = input("답변", "꼬리답1", "꼬리답2");

        // when
        int answered = in.answeredCount();

        // then
        assertThat(in.totalCount()).isEqualTo(3);
        assertThat(answered).isEqualTo(3);
        assertThat(in.skippedCount()).isZero();
        assertThat(in.completeness()).isEqualTo(1.0);
        assertThat(in.isFullySkipped()).isFalse();
    }

    @Test
    @DisplayName("3개 중 1개를 건너뛰면 완성도 2/3, 건너뜀 1")
    void partiallySkipped() {
        // given
        MainQuestionInput in = input("답변", "꼬리답", "   ");   // 빈칸 = 건너뜀

        // when
        double completeness = in.completeness();

        // then
        assertThat(in.answeredCount()).isEqualTo(2);
        assertThat(in.skippedCount()).isEqualTo(1);
        assertThat(completeness).isEqualTo(2.0 / 3.0);
        assertThat(in.isFullySkipped()).isFalse();
    }

    @Test
    @DisplayName("모든 답이 비면 완전 건너뜀으로 판정")
    void fullySkipped() {
        // given
        MainQuestionInput in = input(null, null, "");

        // when
        boolean fullySkipped = in.isFullySkipped();

        // then
        assertThat(fullySkipped).isTrue();
        assertThat(in.answeredCount()).isZero();
        assertThat(in.completeness()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("꼬리가 없으면 총 1개(메인)로 계산된다")
    void mainOnly() {
        // given
        MainQuestionInput in = input("답변");   // 꼬리 없음

        // when
        int total = in.totalCount();

        // then
        assertThat(total).isEqualTo(1);
        assertThat(in.completeness()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("scaledBy는 완성도 비율만큼 축 점수를 낮춘다(반올림)")
    void scaledBy() {
        // given
        MainQuestionScore score = new MainQuestionScore(
                "제목", Map.of("ACC", 90, "DEPTH", 100), null);

        // when
        MainQuestionScore scaled = score.scaledBy(2.0 / 3.0);

        // then
        assertThat(scaled.axisScores().get("ACC")).isEqualTo(60);   // 90*0.667≈60
        assertThat(scaled.axisScores().get("DEPTH")).isEqualTo(67); // 100*0.667≈67
        assertThat(scaled.shortTitle()).isEqualTo("제목");           // 나머지는 유지
    }

    @Test
    @DisplayName("skipped는 모든 축 0점 + 안내 피드백 + 제목 20자 제한")
    void skipped() {
        // given
        List<Axis> axes = List.of(
                new Axis("ACC", "정확성", 50),
                new Axis("DEPTH", "깊이", 50));
        String longQuestion = "가나다라마바사아자차카타파하가나다라마바사아자차카타"; // 20자 초과

        // when
        MainQuestionScore score = MainQuestionScore.skipped(axes, longQuestion);

        // then
        assertThat(score.axisScores()).containsValues(0, 0);
        assertThat(score.content().weak()).isEqualTo("답변을 건너뛰어 평가할 수 없습니다.");
        assertThat(score.shortTitle()).hasSize(20);
    }
}