package com.benly.document.service;

import com.benly.document.dto.DocumentResponse;
import com.benly.document.entity.Document;
import com.benly.document.exception.DocumentErrorCode;
import com.benly.document.repository.DocumentRepository;
import com.benly.document.storage.S3StorageService;
import com.benly.global.exception.BusinessException;
import com.benly.user.entity.User;
import com.benly.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private S3StorageService s3StorageService;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DocumentService documentService;

    private User userWithId(Long id) {
        User user = User.of("kakao-" + id, "닉네임" + id);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Document documentOwnedBy(Long docId, User owner) {
        Document doc = Document.create(owner, "지원서.pdf", "documents/1/uuid.pdf");
        ReflectionTestUtils.setField(doc, "id", docId);
        return doc;
    }

    @Test
    @DisplayName("PDF를 업로드하면 S3에 올리고 Document를 저장한다")
    void uploadSuccess() {
        // given
        byte[] pdfBytes = "%PDF-1.4\n실제내용".getBytes();
        MultipartFile file = new MockMultipartFile(
                "file", "지원서.pdf", MediaType.APPLICATION_PDF_VALUE, pdfBytes);
        given(s3StorageService.upload(file, 1L)).willReturn("documents/1/uuid.pdf");
        given(userRepository.getReferenceById(1L)).willReturn(userWithId(1L));
        given(documentRepository.save(any(Document.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        DocumentResponse response = documentService.upload(1L, file);

        // then
        assertThat(response.fileName()).isEqualTo("지원서.pdf");
        verify(s3StorageService).upload(file, 1L);
        verify(documentRepository).save(any(Document.class));
    }

    @Test
    @DisplayName("PDF가 아니면 422로 거부하고 S3에 올리지 않는다")
    void uploadRejectsNonPdf() {
        // given
        MultipartFile file = new MockMultipartFile(
                "file", "image.png", MediaType.IMAGE_PNG_VALUE, "img".getBytes());

        // when & then
        assertThatThrownBy(() -> documentService.upload(1L, file))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(DocumentErrorCode.INVALID_FILE_TYPE));

        verify(s3StorageService, never()).upload(any(), any());
    }

    @Test
    @DisplayName("Content-Type만 PDF로 위조한 파일(실제 PNG)은 422로 거부한다")
    void uploadRejectsFakePdf() {
        byte[] pngBytes = {(byte)0x89, 'P', 'N', 'G', 0x0D, 0x0A};
        MultipartFile file = new MockMultipartFile(
                "file", "fake.pdf", MediaType.APPLICATION_PDF_VALUE, pngBytes);

        assertThatThrownBy(() -> documentService.upload(1L, file))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(DocumentErrorCode.INVALID_FILE_TYPE));

        verify(s3StorageService, never()).upload(any(), any());
    }

    @Test
    @DisplayName("빈 파일이면 422로 거부한다")
    void uploadRejectsEmptyFile() {
        // given
        MultipartFile file = new MockMultipartFile(
                "file", "empty.pdf", MediaType.APPLICATION_PDF_VALUE, new byte[0]);

        // when & then
        assertThatThrownBy(() -> documentService.upload(1L, file))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(DocumentErrorCode.INVALID_FILE_TYPE));

        verify(s3StorageService, never()).upload(any(), any());
    }

    @Test
    @DisplayName("DB 저장이 실패하면 S3에 올린 객체를 되돌리고(삭제) UPLOAD_FAILED를 던진다")
    void uploadRollsBackS3WhenDbFails() {
        // given
        byte[] pdfBytes = "%PDF-1.4\n내용".getBytes();
        MultipartFile file = new MockMultipartFile(
                "file", "지원서.pdf", MediaType.APPLICATION_PDF_VALUE, pdfBytes);
        given(s3StorageService.upload(file, 1L)).willReturn("documents/1/uuid.pdf");
        given(userRepository.getReferenceById(1L)).willReturn(userWithId(1L));
        given(documentRepository.save(any(Document.class)))
                .willThrow(new RuntimeException("DB 저장 실패"));

        // when & then
        assertThatThrownBy(() -> documentService.upload(1L, file))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(DocumentErrorCode.UPLOAD_FAILED));

        verify(s3StorageService).delete("documents/1/uuid.pdf");
    }


    @Test
    @DisplayName("본인 서류를 삭제하면 S3 객체를 지우고 소프트삭제한다")
    void deleteSuccess() {
        // given
        User owner = userWithId(1L);
        Document doc = documentOwnedBy(10L, owner);
        given(documentRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(doc));

        // when
        documentService.delete(1L, 10L);

        // then
        verify(s3StorageService).delete("documents/1/uuid.pdf");
        assertThat(doc.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("남의 서류를 삭제하려 하면 403이고 S3를 건드리지 않는다")
    void deleteForbidden() {
        // given
        User owner = userWithId(1L);
        Document doc = documentOwnedBy(10L, owner);
        given(documentRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(doc));

        // when & then
        assertThatThrownBy(() -> documentService.delete(999L, 10L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(DocumentErrorCode.DOCUMENT_FORBIDDEN));

        verify(s3StorageService, never()).delete(anyString());
    }

    @Test
    @DisplayName("없는 서류를 삭제하려 하면 404")
    void deleteNotFound() {
        // given
        given(documentRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> documentService.delete(1L, 10L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(DocumentErrorCode.DOCUMENT_NOT_FOUND));

        verify(s3StorageService, never()).delete(anyString());
    }


    @Test
    @DisplayName("본인 서류의 이름을 수정하면 파일명만 바뀌고 S3는 건드리지 않는다")
    void renameSuccess() {
        // given
        User owner = userWithId(1L);
        Document doc = documentOwnedBy(10L, owner);
        given(documentRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(doc));

        // when
        DocumentResponse response = documentService.rename(1L, 10L, "새이름.pdf");

        // then
        assertThat(response.fileName()).isEqualTo("새이름.pdf");
        verify(s3StorageService, never()).delete(anyString());
    }

    @Test
    @DisplayName("남의 서류 이름을 수정하려 하면 403")
    void renameForbidden() {
        // given
        User owner = userWithId(1L);
        Document doc = documentOwnedBy(10L, owner);
        given(documentRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(doc));

        // when & then
        assertThatThrownBy(() -> documentService.rename(999L, 10L, "새이름.pdf"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(DocumentErrorCode.DOCUMENT_FORBIDDEN));
    }
}
