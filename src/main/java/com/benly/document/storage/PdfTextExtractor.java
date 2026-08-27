package com.benly.document.storage;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class PdfTextExtractor {

    private static final int MAX_LENGTH = 5000;   // 너무 길면 자름 (토큰 절약)

    public String extract(byte[] pdfBytes) {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            // 너무 길면 자르기 (Claude 토큰 제한 + 비용)
            if (text.length() > MAX_LENGTH) {
                text = text.substring(0, MAX_LENGTH);
            }
            return text.trim();
        } catch (IOException e) {
            log.warn("PDF 텍스트 추출 실패", e);
            return null;   // 실패해도 질문 생성은 계속 (서류 없이)
        }
    }
}