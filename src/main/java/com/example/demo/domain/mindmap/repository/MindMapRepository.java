package com.example.demo.domain.mindmap.repository;

import com.example.demo.domain.mindmap.entity.MindMap;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MindMapRepository extends JpaRepository<MindMap, Long> {
}
