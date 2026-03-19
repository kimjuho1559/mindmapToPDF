package com.example.demo.domain.mindmap.repository;

import com.example.demo.domain.mindmap.entity.ConceptNode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConceptNodeRepository extends JpaRepository<ConceptNode, Long> {

    List<ConceptNode> findByMindMapId(Long mindMapId);

    Optional<ConceptNode> findByMindMapIdAndLabel(Long mindMapId, String label);
}
