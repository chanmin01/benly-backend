package com.benly.question.service;

import com.benly.question.client.ClaudeClient;
import com.benly.question.entity.Question;
import com.benly.question.entity.QuestionSourceType;
import com.benly.question.entity.SeedQuestion;
import com.benly.question.repository.QuestionRepository;
import com.benly.question.repository.SeedQuestionRepository;
import com.benly.session.entity.Session;
import com.benly.session.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionGenerationService {

    private final ClaudeClient claudeClient;
    private final SeedQuestionRepository seedQuestionRepository;
    private final QuestionRepository questionRepository;
    private final SessionRepository sessionRepository;

    @Transactional
    public void generate(Long sessionId, String jd) {
        Session session = sessionRepository.findById(sessionId).orElse(null);

        List<String> questionTexts;
        QuestionSourceType sourceType;

        try{
            questionTexts = claudeClient.generateQuestions(
                    session.getCompanyType(), session.getStage(), session.getJobTitle(), jd);
            sourceType = QuestionSourceType.CLAUDE;
        } catch (Exception e){
            List<SeedQuestion> seeds = seedQuestionRepository.findByCompanyTypeAndStage(session.getCompanyType(), session.getStage());
            questionTexts = seeds.stream().map(SeedQuestion::getContent).toList();
            sourceType = QuestionSourceType.SEED_FALLBACK;
        }

        int seq = 1;
        for (String text : questionTexts) {
            Question question = Question.createMain(session, seq++, text, sourceType);
            questionRepository.save(question);
        }
        session.markReady();
    }
}
