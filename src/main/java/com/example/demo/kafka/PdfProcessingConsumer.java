package com.example.demo.kafka;

import com.example.demo.ai.OpenAiAnalysisService;
import com.example.demo.ai.dto.ConceptGraphDto;
import com.example.demo.domain.mindmap.entity.ConceptEdge;
import com.example.demo.domain.mindmap.entity.ConceptNode;
import com.example.demo.domain.mindmap.entity.MindMap;
import com.example.demo.domain.mindmap.entity.MindMapStatus;
import com.example.demo.domain.mindmap.repository.ConceptEdgeRepository;
import com.example.demo.domain.mindmap.repository.ConceptNodeRepository;
import com.example.demo.domain.mindmap.repository.MindMapRepository;
import com.example.demo.domain.pdf.entity.PdfDocument;
import com.example.demo.domain.pdf.repository.PdfDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PdfProcessingConsumer {

    private final MindMapRepository mindMapRepository;
    private final PdfDocumentRepository pdfDocumentRepository;
    private final ConceptNodeRepository conceptNodeRepository;
    private final ConceptEdgeRepository conceptEdgeRepository;
    private final OpenAiAnalysisService ollamaAnalysisService;

    @KafkaListener(topics = "pdf-processing", groupId = "mindmap-group")
    @Transactional
    public void consume(String mindMapIdStr) {
        Long mindMapId = Long.parseLong(mindMapIdStr);
        log.info("PDF 처리 이벤트 수신 - mindMapId: {}", mindMapId);

        MindMap mindMap = mindMapRepository.findById(mindMapId)
                .orElseThrow(() -> new IllegalArgumentException("MindMap not found: " + mindMapId));

        mindMap.setStatus(MindMapStatus.PROCESSING);
        mindMapRepository.save(mindMap);

        try {
            PdfDocument pdfDocument = pdfDocumentRepository.findByMindMapId(mindMapId)
                    .orElseThrow(() -> new IllegalArgumentException("PdfDocument not found for mindMapId: " + mindMapId));

            ConceptGraphDto graph = ollamaAnalysisService.extractConcepts(
                    pdfDocument.getRawText(), mindMap.getSubject(), mindMap.getDetailLevel());

            if (graph.isEmpty()) {
                log.warn("개념 추출 결과가 비어있습니다 - mindMapId: {}", mindMapId);
                mindMap.setStatus(MindMapStatus.FAILED);
                return;
            }

            // 노드 저장 (label → entity 매핑 유지)
            Map<String, ConceptNode> nodeMap = new HashMap<>();
            for (ConceptGraphDto.NodeDto nodeDto : graph.getNodes()) {
                ConceptNode node = ConceptNode.create(mindMap, nodeDto.getLabel(), nodeDto.getDescription(), nodeDto.getCategory());
                ConceptNode saved = conceptNodeRepository.save(node);
                nodeMap.put(nodeDto.getLabel(), saved);
            }

            // 엣지 저장
            for (ConceptGraphDto.EdgeDto edgeDto : graph.getEdges()) {
                ConceptNode sourceNode = nodeMap.get(edgeDto.getFrom());
                ConceptNode targetNode = nodeMap.get(edgeDto.getTo());

                if (sourceNode == null || targetNode == null) {
                    log.warn("엣지 생성 스킵 - 노드를 찾을 수 없음: {} -> {}", edgeDto.getFrom(), edgeDto.getTo());
                    continue;
                }

                ConceptEdge edge = ConceptEdge.create(mindMap, sourceNode, targetNode, edgeDto.getLabel());
                conceptEdgeRepository.save(edge);
            }

            mindMap.setStatus(MindMapStatus.DONE);
            log.info("마인드맵 생성 완료 - mindMapId: {}, 노드: {}개, 엣지: {}개",
                    mindMapId, nodeMap.size(), graph.getEdges().size());

        } catch (Exception e) {
            log.error("PDF 처리 실패 - mindMapId: {}, 오류: {}", mindMapId, e.getMessage());
            mindMap.setStatus(MindMapStatus.FAILED);
        }
    }
}
