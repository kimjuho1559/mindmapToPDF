package com.example.demo.domain.pdf.controller;

import com.example.demo.domain.pdf.dto.PdfUploadResponse;
import com.example.demo.domain.pdf.entity.PdfDocument;
import com.example.demo.domain.pdf.repository.PdfDocumentRepository;
import com.example.demo.domain.pdf.service.PdfService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/pdf")
@RequiredArgsConstructor
public class PdfController {

    private final PdfService pdfService;
    private final PdfDocumentRepository pdfDocumentRepository;

    /**
     * PDF 업로드 → 텍스트 추출 → Kafka 이벤트 발행 → 비동기 AI 분석
     *
     * POST /api/pdf/upload
     * Content-Type: multipart/form-data
     * Params: file (PDF), subject (과목명, 선택)
     */
    @PostMapping("/upload")
    public ResponseEntity<PdfUploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "subject", defaultValue = "") String subject,
            @RequestParam(value = "detailLevel", defaultValue = "NORMAL") String detailLevel) throws IOException {

        PdfUploadResponse response = pdfService.upload(file, subject, detailLevel);
        return ResponseEntity.ok(response);
    }

    /**
     * PDF 파일 서빙
     * GET /api/pdf/file/{mindMapId}
     */
    @GetMapping("/file/{mindMapId}")
    public ResponseEntity<Resource> getPdfFile(@PathVariable Long mindMapId) throws IOException {
        PdfDocument doc = pdfDocumentRepository.findByMindMapId(mindMapId)
                .orElseThrow(() -> new IllegalArgumentException("PDF not found for mindMapId: " + mindMapId));

        Path filePath = Paths.get(doc.getFilePath());
        Resource resource = new UrlResource(filePath.toUri());

        String encodedName = java.net.URLEncoder.encode(
                doc.getOriginalFilename(), java.nio.charset.StandardCharsets.UTF_8
        ).replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename*=UTF-8''" + encodedName)
                .body(resource);
    }
}
