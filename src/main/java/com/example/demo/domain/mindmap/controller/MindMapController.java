package com.example.demo.domain.mindmap.controller;

import com.example.demo.domain.mindmap.dto.MindMapResponse;
import com.example.demo.domain.mindmap.service.MindMapService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mindmap")
@RequiredArgsConstructor
public class MindMapController {

    private final MindMapService mindMapService;

    /**
     * 마인드맵 단건 조회 (노드 + 엣지 포함)
     * GET /api/mindmap/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<MindMapResponse> getMindMap(@PathVariable Long id) {
        return ResponseEntity.ok(mindMapService.getMindMap(id));
    }

    /**
     * 전체 마인드맵 목록 조회
     * GET /api/mindmap/list
     */
    @GetMapping("/list")
    public ResponseEntity<List<MindMapResponse>> getAllMindMaps() {
        return ResponseEntity.ok(mindMapService.getAllMindMaps());
    }

    /**
     * 마인드맵 삭제
     * DELETE /api/mindmap/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMindMap(@PathVariable Long id) {
        mindMapService.deleteMindMap(id);
        return ResponseEntity.noContent().build();
    }
}
