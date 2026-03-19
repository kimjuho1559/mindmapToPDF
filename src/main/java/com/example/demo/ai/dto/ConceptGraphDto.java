package com.example.demo.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

// Ollama가 반환하는 개념 그래프 JSON을 파싱하는 DTO
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConceptGraphDto {

    private List<NodeDto> nodes = new ArrayList<>();
    private List<EdgeDto> edges = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NodeDto {
        private String label;
        private String description;
        private String category;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EdgeDto {
        private String from;
        private String to;
        private String label;
    }

    public static ConceptGraphDto empty() {
        return new ConceptGraphDto();
    }

    public boolean isEmpty() {
        return nodes.isEmpty();
    }
}
