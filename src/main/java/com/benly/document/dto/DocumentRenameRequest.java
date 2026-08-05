package com.benly.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DocumentRenameRequest(
        @NotBlank(message = "파일명은 비어 있을 수 없습니다.")
        @Size(max = 255, message = "파일명은 255자를 넘을 수 없습니다.")
        String fileName
) {
}
