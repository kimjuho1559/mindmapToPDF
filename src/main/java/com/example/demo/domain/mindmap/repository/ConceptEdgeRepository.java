package com.example.demo.domain.mindmap.repository;

import com.example.demo.domain.mindmap.entity.ConceptEdge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConceptEdgeRepository extends JpaRepository<ConceptEdge, Long> {

    List<ConceptEdge> findByMindMapId(Long mindMapId);
}
