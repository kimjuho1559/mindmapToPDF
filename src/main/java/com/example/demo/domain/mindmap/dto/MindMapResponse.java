package com.example.demo.domain.mindmap.dto;

import com.example.demo.domain.mindmap.entity.ConceptEdge;
import com.example.demo.domain.mindmap.entity.ConceptNode;
import com.example.demo.domain.mindmap.entity.MindMap;

import java.util.List;

public record MindMapResponse(
        Long id,
        String title,
        String subject,
        String status,
        String pdfUrl,
        List<NodeDto> nodes,
        List<EdgeDto> edges
) {

    public record NodeDto(
            Long id,
            String label,
            String description,
            String category
    ) {
        public static NodeDto from(ConceptNode node) {
            return new NodeDto(node.getId(), node.getLabel(), node.getDescription(), node.getCategory());
        }
    }

    public record EdgeDto(
            Long id,
            Long from,
            Long to,
            String label
    ) {
        public static EdgeDto from(ConceptEdge edge) {
            return new EdgeDto(
                    edge.getId(),
                    edge.getSourceNode().getId(),
                    edge.getTargetNode().getId(),
                    edge.getLabel()
            );
        }
    }

    public static MindMapResponse from(MindMap mindMap, List<ConceptNode> nodes, List<ConceptEdge> edges) {
        return new MindMapResponse(
                mindMap.getId(),
                mindMap.getTitle(),
                mindMap.getSubject(),
                mindMap.getStatus().name(),
                "/api/pdf/file/" + mindMap.getId(),
                nodes.stream().map(NodeDto::from).toList(),
                edges.stream().map(EdgeDto::from).toList()
        );
    }
}
