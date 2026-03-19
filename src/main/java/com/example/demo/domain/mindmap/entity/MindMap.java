package com.example.demo.domain.mindmap.entity;

import com.example.demo.domain.pdf.entity.PdfDocument;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "mind_maps")
@Getter
@Setter
@NoArgsConstructor
public class MindMap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MindMapStatus status = MindMapStatus.PENDING;

    @OneToOne(mappedBy = "mindMap", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private PdfDocument pdfDocument;

    @OneToMany(mappedBy = "mindMap", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ConceptNode> nodes = new ArrayList<>();

    @OneToMany(mappedBy = "mindMap", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ConceptEdge> edges = new ArrayList<>();

    @Column(nullable = false)
    private String detailLevel = "NORMAL";

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public static MindMap create(String title, String subject, String detailLevel) {
        MindMap mindMap = new MindMap();
        mindMap.title = title;
        mindMap.subject = subject;
        mindMap.detailLevel = (detailLevel != null && !detailLevel.isBlank()) ? detailLevel : "NORMAL";
        return mindMap;
    }
}
