package com.example.demo.domain.mindmap.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "concept_nodes")
@Getter
@Setter
@NoArgsConstructor
public class ConceptNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mind_map_id", nullable = false)
    private MindMap mindMap;

    @Column(nullable = false)
    private String label;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String category;

    public static ConceptNode create(MindMap mindMap, String label, String description, String category) {
        ConceptNode node = new ConceptNode();
        node.mindMap = mindMap;
        node.label = label;
        node.description = description;
        node.category = category;
        return node;
    }
}
