package com.example.demo.domain.pdf.entity;

import com.example.demo.domain.mindmap.entity.MindMap;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "pdf_documents")
@Getter
@Setter
@NoArgsConstructor
public class PdfDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mind_map_id", nullable = false)
    private MindMap mindMap;

    @Column(nullable = false)
    private String originalFilename;

    @Column(nullable = false)
    private String filePath;

    @Column(columnDefinition = "TEXT")
    private String rawText;

    @Column(nullable = false)
    private LocalDateTime uploadedAt = LocalDateTime.now();

    public static PdfDocument create(MindMap mindMap, String originalFilename, String filePath) {
        PdfDocument doc = new PdfDocument();
        doc.mindMap = mindMap;
        doc.originalFilename = originalFilename;
        doc.filePath = filePath;
        return doc;
    }
}
