package com.benly.question.service;

import com.benly.document.entity.Document;
import com.benly.document.repository.DocumentRepository;
import com.benly.document.storage.PdfTextExtractor;
import com.benly.document.storage.S3StorageService;
import com.benly.question.client.ClaudeClient;
import com.benly.question.entity.Question;
import com.benly.question.entity.QuestionSourceType;
import com.benly.question.entity.SeedQuestion;
import com.benly.question.repository.QuestionRepository;
import com.benly.question.repository.SeedQuestionRepository;
import com.benly.session.entity.Session;
import com.benly.session.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionGenerationService {

    private final ClaudeClient claudeClient;
    private final SeedQuestionRepository seedQuestionRepository;
    private final QuestionRepository questionRepository;
    private final SessionRepository sessionRepository;
    private final DocumentRepository documentRepository;
    private final S3StorageService s3StorageService;
    private final PdfTextExtractor pdfTextExtractor;

    @Async
    @Transactional
    public void generate(Long sessionId, String jd, Long docId) {
        Session session = sessionRepository.findById(sessionId).orElseThrow(
                () -> new IllegalArgumentException("해당 세션을 찾을 수 없습니다. ID: " + sessionId));

        try {

            String docText = extractDocumentText(docId);
            List<String> questionTexts;
            QuestionSourceType sourceType;

            try {
                questionTexts = claudeClient.generateQuestions(
                        session.getCompanyType(), session.getStage(), session.getJobTitle(), jd, docText);
                sourceType = QuestionSourceType.CLAUDE;
            } catch (Exception e) {
                List<SeedQuestion> seeds = seedQuestionRepository.findTop5ByCompanyTypeAndStage(session.getCompanyType(), session.getStage());
                questionTexts = seeds.stream().map(SeedQuestion::getContent).toList();
                sourceType = QuestionSourceType.SEED_FALLBACK;
            }

            int seq = 1;
            for (String text : questionTexts) {
                Question question = Question.createMain(session, seq++, text, sourceType);
                questionRepository.save(question);
            }
            session.markReady();

        } catch (Exception e) {
            session.markFailed();
        }
    }

    // 서류 텍스트 추출 (실패해도 null 반환, 질문 생성은 계속)
    private String extractDocumentText(Long docId) {
        if (docId == null) {
            return null;   // 서류 없으면 null
        }
        try {
            Document document = documentRepository.findByIdAndDeletedAtIsNull(docId)
                    .orElse(null);
            if (document == null) {
                return null;
            }
            byte[] pdfBytes = s3StorageService.download(document.getStorageKey());
            return pdfTextExtractor.extract(pdfBytes);
        } catch (Exception e) {
            log.warn("서류 텍스트 추출 실패, 서류 없이 질문 생성. docId={}", docId, e);
            return null;   // 실패해도 질문 생성 계속
        }
    }
}
