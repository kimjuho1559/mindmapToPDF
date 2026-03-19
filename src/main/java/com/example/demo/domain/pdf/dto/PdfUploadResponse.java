package com.example.demo.domain.pdf.dto;

public record PdfUploadResponse(
        Long mindMapId,
        String title,
        String status,
        String message
) {
    public static PdfUploadResponse of(Long mindMapId, String title) {
        return new PdfUploadResponse(mindMapId, title, "PENDING", "PDF 업로드 완료. AI 분석을 시작합니다.");
    }
}
