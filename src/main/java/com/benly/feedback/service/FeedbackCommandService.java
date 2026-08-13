package com.benly.feedback.service;

import com.benly.feedback.client.FeedbackScoringClient;
import com.benly.feedback.client.FeedbackScoringClient.ScoringContext;
import com.benly.feedback.dto.FeedbackContent;
import com.benly.feedback.dto.ScoringResult.MainQuestionInput;
import com.benly.feedback.dto.ScoringResult.MainQuestionScore;
import com.benly.feedback.dto.ScoringResult.SessionSummary;
import com.benly.feedback.dto.ScoringResult.TailInput;
import com.benly.feedback.entity.*;
import com.benly.feedback.repository.*;
import com.benly.question.entity.Answer;
import com.benly.question.entity.Question;
import com.benly.session.entity.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackCommandService {

    private final FeedbackScoringClient scoringClient;
    private final FeedbackContentParser contentParser;
    private final SessionReadRepository sessionReadRepository;
    private final SessionFeedbackRepository sessionFeedbackRepository;
    private final FeedbackRepository feedbackRepository;
    private final ScoreAxisRepository scoreAxisRepository;
    private final QuestionReadRepository questionReadRepository;
    private final AnswerReadRepository answerReadRepository;

    @Async
    @Transactional
    public void score(Long sessionId) {
        SessionFeedback sf = prepareSessionFeedback(sessionId);
        try {
            Session session = sf.getSession();
            AxisSet axisSet = AxisSet.resolve(session.getStage(), session.getCompanyType());
            List<Axis> axes = axisSet.axes();
            ScoringContext ctx = new ScoringContext(
                    session.getCompanyType(), session.getStage(), session.getJobTitle());

            List<Question> allQuestions = questionReadRepository.findBySession_IdOrderBySeqAsc(sessionId);
            Map<Long, String> transcriptByQuestion = new HashMap<>();
            for (Answer a : answerReadRepository.findBySessionId(sessionId)) {
                if (a.getQuestion() != null) {
                    transcriptByQuestion.put(a.getQuestion().getId(), a.getTranscript());
                }
            }

            Map<Long, List<Question>> tailsByParent = allQuestions.stream()
                    .filter(q -> q.getParent() != null)
                    .collect(Collectors.groupingBy(q -> q.getParent().getId()));

            List<Question> mains = allQuestions.stream()
                    .filter(Question::isMain)
                    .sorted(Comparator.comparing(Question::getSeq))
                    .toList();

            List<MainQuestionScore> results = new ArrayList<>();
            List<Integer> topicScores = new ArrayList<>();
            int totalSkipped = 0;

            for (Question main : mains) {
                MainQuestionInput input = buildInput(main, tailsByParent, transcriptByQuestion);

                int totalQuestions = 1 + input.tails().size();
                int answered = (isAnswered(input.answer()) ? 1 : 0)
                        + (int) input.tails().stream().filter(t -> isAnswered(t.answer())).count();
                totalSkipped += (totalQuestions - answered);
                double completeness = (double) answered / totalQuestions;

                MainQuestionScore score;
                Map<String, Integer> scaledAxes;

                if (answered == 0) {
                    score = skippedScore(axes, main);
                    scaledAxes = score.axisScores();
                } else {
                    MainQuestionScore raw = scoringClient.scoreMainQuestion(axes, ctx, input);
                    scaledAxes = scaleAxes(raw.axisScores(), completeness);
                    score = new MainQuestionScore(raw.shortTitle(), scaledAxes, raw.content());
                }

                int topicScore = axisSet.weightedScore(scaledAxes);
                saveQuestionScore(main, score, topicScore);

                results.add(score);
                topicScores.add(topicScore);
            }

            int totalScore = topicScores.isEmpty() ? 0
                    : Math.round((float) topicScores.stream().mapToInt(Integer::intValue).sum() / topicScores.size());

            SessionSummary summary = scoringClient.summarize(ctx, totalScore, results, totalSkipped);

            sf.complete(totalScore, summary.summary(),
                    summary.keyCoachingWeakness(), summary.keyCoachingAction());

            log.info("채점 완료 sessionId={}, totalScore={}", sessionId, totalScore);

        } catch (Exception e) {
            log.error("채점 실패 sessionId={}", sessionId, e);
            sf.fail();
        }
    }

    private SessionFeedback prepareSessionFeedback(Long sessionId) {
        return sessionFeedbackRepository.findBySession_Id(sessionId)
                .map(existing -> {
                    scoreAxisRepository.deleteBySessionId(sessionId);
                    feedbackRepository.deleteBySessionId(sessionId);
                    existing.resetForRescore();
                    return existing;
                })
                .orElseGet(() -> {
                    Session session = sessionReadRepository.findById(sessionId)
                            .orElseThrow(() -> new IllegalArgumentException("세션 없음: " + sessionId));
                    return sessionFeedbackRepository.save(SessionFeedback.startScoring(session));
                });
    }

    private MainQuestionInput buildInput(Question main,
                                         Map<Long, List<Question>> tailsByParent,
                                         Map<Long, String> transcriptByQuestion) {
        List<Question> tailQs = new ArrayList<>(tailsByParent.getOrDefault(main.getId(), List.of()));
        tailQs.sort(Comparator.comparing(Question::getSeq));

        List<TailInput> tails = tailQs.stream()
                .map(tq -> new TailInput(
                        tq.getStrategy(),
                        tq.getContent(),
                        transcriptByQuestion.get(tq.getId())))
                .toList();

        return new MainQuestionInput(
                main.getSeq(),
                main.getContent(),
                transcriptByQuestion.get(main.getId()),
                tails);
    }

    private boolean isAnswered(String transcript) {
        return transcript != null && !transcript.isBlank();
    }

    private Map<String, Integer> scaleAxes(Map<String, Integer> axisScores, double completeness) {
        Map<String, Integer> scaled = new HashMap<>();
        for (Map.Entry<String, Integer> e : axisScores.entrySet()) {
            scaled.put(e.getKey(), (int) Math.round(e.getValue() * completeness));
        }
        return scaled;
    }

    private MainQuestionScore skippedScore(List<Axis> axes, Question main) {
        Map<String, Integer> zeros = new HashMap<>();
        for (Axis axis : axes) {
            zeros.put(axis.code(), 0);
        }
        FeedbackContent content = new FeedbackContent(
                null,
                "답변을 건너뛰어 평가할 수 없습니다.",
                "다음에는 짧게라도 답변을 시도해 보세요.",
                null,
                null,
                List.of()
        );
        String shortTitle = main.getContent() == null ? "건너뛴 질문"
                : main.getContent().substring(0, Math.min(20, main.getContent().length()));
        return new MainQuestionScore(shortTitle, zeros, content);
    }

    private void saveQuestionScore(Question main, MainQuestionScore score, int topicScore) {
        main.assignShortTitle(score.shortTitle());

        String contentJson = contentParser.toJson(score.content());

        Feedback feedback = Feedback.create(main, contentJson, topicScore);
        feedbackRepository.save(feedback);

        for (Map.Entry<String, Integer> e : score.axisScores().entrySet()) {
            scoreAxisRepository.save(ScoreAxis.create(feedback, e.getKey(), e.getValue()));
        }
    }
}
