package com.example.demo.domain.pdf.repository;

import com.example.demo.domain.pdf.entity.PdfDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PdfDocumentRepository extends JpaRepository<PdfDocument, Long> {

    Optional<PdfDocument> findByMindMapId(Long mindMapId);
}
