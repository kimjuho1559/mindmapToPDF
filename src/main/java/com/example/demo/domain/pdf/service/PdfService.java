package com.example.demo.domain.pdf.service;

import com.example.demo.domain.mindmap.entity.MindMap;
import com.example.demo.domain.mindmap.repository.MindMapRepository;
import com.example.demo.domain.pdf.dto.PdfUploadResponse;
import com.example.demo.domain.pdf.entity.PdfDocument;
import com.example.demo.domain.pdf.repository.PdfDocumentRepository;
import com.example.demo.kafka.PdfProcessingProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfService {

    private final MindMapRepository mindMapRepository;
    private final PdfDocumentRepository pdfDocumentRepository;
    private final PdfProcessingProducer pdfProcessingProducer;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Transactional
    public PdfUploadResponse upload(MultipartFile file, String subject, String detailLevel) throws IOException {
        validatePdfFile(file);

        // 1. MindMap 생성
        String title = extractTitle(file.getOriginalFilename());
        MindMap mindMap = MindMap.create(title, subject, detailLevel);
        mindMapRepository.save(mindMap);

        // 2. 파일 저장
        String savedPath = saveFile(file);

        // 3. PDF 텍스트 추출
        String rawText = extractText(file);
        log.info("PDF 텍스트 추출 완료 - 길이: {}", rawText.length());

        // 4. PdfDocument 저장
        PdfDocument pdfDocument = PdfDocument.create(mindMap, file.getOriginalFilename(), savedPath);
        pdfDocument.setRawText(rawText);
        pdfDocumentRepository.save(pdfDocument);

        // 5. Kafka 이벤트 발행 (비동기 AI 분석 트리거)
        pdfProcessingProducer.sendMindMapId(mindMap.getId());

        return PdfUploadResponse.of(mindMap.getId(), title);
    }

    private String extractText(MultipartFile file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            // PostgreSQL UTF-8은 null byte(0x00)를 허용하지 않으므로 제거
            return text.replace("\u0000", "");
        }
    }

    private String saveFile(MultipartFile file) throws IOException {
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(filename);
        Files.write(filePath, file.getBytes());

        return filePath.toString();
    }

    private void validatePdfFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다.");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("PDF 파일만 업로드 가능합니다.");
        }
    }

    private String extractTitle(String filename) {
        if (filename == null) return "제목 없음";
        return filename.replaceAll("\\.pdf$", "").replaceAll("\\.PDF$", "");
    }
}
