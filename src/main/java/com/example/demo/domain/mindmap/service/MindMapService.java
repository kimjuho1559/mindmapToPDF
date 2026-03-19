package com.example.demo.domain.mindmap.service;

import com.example.demo.domain.mindmap.dto.MindMapResponse;
import com.example.demo.domain.mindmap.entity.ConceptEdge;
import com.example.demo.domain.mindmap.entity.ConceptNode;
import com.example.demo.domain.mindmap.entity.MindMap;
import com.example.demo.domain.mindmap.repository.ConceptEdgeRepository;
import com.example.demo.domain.mindmap.repository.ConceptNodeRepository;
import com.example.demo.domain.mindmap.repository.MindMapRepository;
import com.example.demo.domain.pdf.entity.PdfDocument;
import com.example.demo.domain.pdf.repository.PdfDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MindMapService {

    private final MindMapRepository mindMapRepository;
    private final ConceptNodeRepository conceptNodeRepository;
    private final ConceptEdgeRepository conceptEdgeRepository;
    private final PdfDocumentRepository pdfDocumentRepository;

    @Cacheable(value = "mindmap", key = "#mindMapId",
            unless = "#result.status == 'PENDING' || #result.status == 'PROCESSING'")
    @Transactional(readOnly = true)
    public MindMapResponse getMindMap(Long mindMapId) {
        MindMap mindMap = mindMapRepository.findById(mindMapId)
                .orElseThrow(() -> new IllegalArgumentException("MindMap not found: " + mindMapId));

        List<ConceptNode> nodes = conceptNodeRepository.findByMindMapId(mindMapId);
        List<ConceptEdge> edges = conceptEdgeRepository.findByMindMapId(mindMapId);

        return MindMapResponse.from(mindMap, nodes, edges);
    }

    @CacheEvict(value = "mindmap", key = "#mindMapId")
    @Transactional
    public void deleteMindMap(Long mindMapId) {
        MindMap mindMap = mindMapRepository.findById(mindMapId)
                .orElseThrow(() -> new IllegalArgumentException("MindMap not found: " + mindMapId));

        // PDF 파일 디스크에서 삭제
        pdfDocumentRepository.findByMindMapId(mindMapId).ifPresent((PdfDocument doc) -> {
            try {
                Files.deleteIfExists(Paths.get(doc.getFilePath()));
            } catch (Exception e) {
                log.warn("PDF 파일 삭제 실패: {}", doc.getFilePath());
            }
        });

        // MindMap 삭제 (CascadeType.ALL로 노드·엣지·PdfDocument 자동 삭제)
        mindMapRepository.delete(mindMap);
        log.info("마인드맵 삭제 완료 - id: {}", mindMapId);
    }

    @Transactional(readOnly = true)
    public List<MindMapResponse> getAllMindMaps() {
        return mindMapRepository.findAll().stream()
                .map(mindMap -> {
                    List<ConceptNode> nodes = conceptNodeRepository.findByMindMapId(mindMap.getId());
                    List<ConceptEdge> edges = conceptEdgeRepository.findByMindMapId(mindMap.getId());
                    return MindMapResponse.from(mindMap, nodes, edges);
                })
                .toList();
    }
}
