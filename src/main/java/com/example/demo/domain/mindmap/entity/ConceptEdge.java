package com.example.demo.domain.mindmap.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "concept_edges")
@Getter
@Setter
@NoArgsConstructor
public class ConceptEdge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mind_map_id", nullable = false)
    private MindMap mindMap;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_node_id", nullable = false)
    private ConceptNode sourceNode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_node_id", nullable = false)
    private ConceptNode targetNode;

    private String label;

    private String relationType;

    public static ConceptEdge create(MindMap mindMap, ConceptNode source, ConceptNode target, String label) {
        ConceptEdge edge = new ConceptEdge();
        edge.mindMap = mindMap;
        edge.sourceNode = source;
        edge.targetNode = target;
        edge.label = label;
        return edge;
    }
}
