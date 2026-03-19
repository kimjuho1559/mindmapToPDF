package com.example.demo.domain.mindmap.entity;

public enum MindMapStatus {
    PENDING,      // 업로드 완료, 처리 대기
    PROCESSING,   // AI 분석 중
    DONE,         // 마인드맵 생성 완료
    FAILED        // 처리 실패
}
