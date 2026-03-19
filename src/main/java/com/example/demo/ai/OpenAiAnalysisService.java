package com.example.demo.ai;

import com.example.demo.ai.dto.ConceptGraphDto;
import tools.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class OpenAiAnalysisService {

    private final RestClient restClient;
    private final String model;
    private final ObjectMapper objectMapper;

    public OpenAiAnalysisService(
            @Value("${openai.api-key}") String apiKey,
            @Value("${openai.model}") String model,
            ObjectMapper objectMapper) {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.openai.com")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
        this.model = model;
        this.objectMapper = objectMapper;
    }

    private record AnalysisConfig(int mainCount, int subCount, int mainTextLen, int subTextLen) {}

    private AnalysisConfig configFor(String detailLevel) {
        return switch (detailLevel == null ? "NORMAL" : detailLevel) {
            case "SIMPLE"   -> new AnalysisConfig(3, 2,  6_000, 2_000);
            case "DETAILED" -> new AnalysisConfig(7, 5, 15_000, 5_000);
            default         -> new AnalysisConfig(5, 4, 10_000, 3_000); // NORMAL
        };
    }

    /**
     * 2단계 호출 방식:
     * 1단계) 주요 개념 추출 (레벨별 개수 다름)
     * 2단계) 세부 개념 전체를 단일 호출로 추출
     */
    public ConceptGraphDto extractConcepts(String pdfText, String subject, String detailLevel) {
        AnalysisConfig cfg = configFor(detailLevel);
        String truncated = smartTruncate(pdfText, cfg.mainTextLen());
        String shortTruncated = smartTruncate(pdfText, cfg.subTextLen());
        String effectiveSubject = (subject != null && !subject.isBlank()) ? subject : "문서 분석";

        log.info("OpenAI 분석 시작 - 모델: {}, 주제: {}, 레벨: {} (주요 {}개, 서브 {}개)",
                model, effectiveSubject, detailLevel, cfg.mainCount(), cfg.subCount());

        List<String> mainConcepts = extractMainConcepts(truncated, effectiveSubject, cfg.mainCount());
        if (mainConcepts.isEmpty()) {
            log.error("주요 개념 추출 실패");
            return ConceptGraphDto.empty();
        }
        log.info("1단계 완료 - 주요 개념: {}", mainConcepts);

        List<ConceptGroup> groups = extractAllSubConcepts(shortTruncated, mainConcepts, cfg.subCount());
        log.info("2단계 완료 - 세부 개념 그룹: {}개", groups.size());

        ConceptGraphDto result = new ConceptGraphDto();

        ConceptGraphDto.NodeDto root = new ConceptGraphDto.NodeDto();
        root.setLabel(effectiveSubject);
        root.setDescription("문서의 핵심 주제");
        root.setCategory("root");
        result.getNodes().add(root);

        for (ConceptGroup group : groups) {
            ConceptGraphDto.NodeDto mainNode = new ConceptGraphDto.NodeDto();
            mainNode.setLabel(group.main());
            mainNode.setDescription("");
            mainNode.setCategory("main");
            result.getNodes().add(mainNode);

            ConceptGraphDto.EdgeDto rootEdge = new ConceptGraphDto.EdgeDto();
            rootEdge.setFrom(effectiveSubject);
            rootEdge.setTo(group.main());
            rootEdge.setLabel("포함");
            result.getEdges().add(rootEdge);

            for (SubConceptItem sub : group.subs()) {
                ConceptGraphDto.NodeDto subNode = new ConceptGraphDto.NodeDto();
                subNode.setLabel(sub.label());
                subNode.setDescription(sub.description());
                subNode.setCategory("sub");
                result.getNodes().add(subNode);

                ConceptGraphDto.EdgeDto subEdge = new ConceptGraphDto.EdgeDto();
                subEdge.setFrom(group.main());
                subEdge.setTo(sub.label());
                subEdge.setLabel("구성요소");
                result.getEdges().add(subEdge);
            }
        }

        log.info("개념 추출 완료 - 노드: {}개, 엣지: {}개",
                result.getNodes().size(), result.getEdges().size());
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<String> extractMainConcepts(String text, String subject, int mainCount) {
        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "concepts", Map.of(
                                "type", "array",
                                "items", Map.of("type", "string")
                        )
                ),
                "required", List.of("concepts"),
                "additionalProperties", false
        );

        String prompt = """
                다음 텍스트는 "%s" 주제를 다루고 있습니다.
                이 텍스트에서 다루는 핵심 주제/챕터를 정확히 %d개만 추출하세요.
                각 항목은 2~5단어의 짧은 개념명으로 작성하세요.

                규칙:
                - 반드시 정확히 %d개
                - 각 개념명은 2~5단어의 짧은 명사구
                - 문장 형태 금지

                텍스트:
                %s
                """.formatted(subject, mainCount, mainCount, text);

        try {
            String raw = callOpenAi(prompt, "extract_concepts", schema);
            Map<String, Object> parsed = objectMapper.readValue(raw, Map.class);
            List<String> concepts = (List<String>) parsed.get("concepts");
            return concepts != null ? concepts : List.of();
        } catch (Exception e) {
            log.error("주요 개념 추출 실패: {}", e.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<ConceptGroup> extractAllSubConcepts(String text, List<String> mainConcepts, int subCount) {
        Map<String, Object> subItemSchema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "label", Map.of("type", "string"),
                        "description", Map.of("type", "string")
                ),
                "required", List.of("label", "description"),
                "additionalProperties", false
        );
        Map<String, Object> groupSchema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "main", Map.of("type", "string"),
                        "subs", Map.of(
                                "type", "array",
                                "items", subItemSchema
                        )
                ),
                "required", List.of("main", "subs"),
                "additionalProperties", false
        );
        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "groups", Map.of(
                                "type", "array",
                                "items", groupSchema
                        )
                ),
                "required", List.of("groups"),
                "additionalProperties", false
        );

        String conceptList = String.join(", ", mainConcepts);
        String prompt = """
                다음 텍스트에서 아래 각 주요 개념별로 세부 개념을 정확히 %d개씩 추출하세요.
                주요 개념 목록: [%s]

                각 세부 개념:
                - label: 2~5단어의 짧은 개념명
                - description: 해당 개념의 정의를 1문장으로

                텍스트:
                %s
                """.formatted(subCount, conceptList, text);

        try {
            String raw = callOpenAi(prompt, "extract_sub_concepts", schema);
            Map<String, Object> parsed = objectMapper.readValue(raw, Map.class);
            List<Map<String, Object>> rawGroups = (List<Map<String, Object>>) parsed.get("groups");
            if (rawGroups == null) return List.of();

            List<ConceptGroup> result = new ArrayList<>();
            for (Map<String, Object> rawGroup : rawGroups) {
                String main = (String) rawGroup.get("main");
                if (main == null || main.isBlank()) continue;

                List<Map<String, Object>> rawSubs = (List<Map<String, Object>>) rawGroup.get("subs");
                List<SubConceptItem> subs = new ArrayList<>();
                if (rawSubs != null) {
                    for (Map<String, Object> rawSub : rawSubs) {
                        String label = (String) rawSub.get("label");
                        String desc = (String) rawSub.getOrDefault("description", "");
                        if (label != null && !label.isBlank()) {
                            subs.add(new SubConceptItem(label, desc));
                        }
                    }
                }
                result.add(new ConceptGroup(main, subs));
            }
            return result;
        } catch (Exception e) {
            log.error("세부 개념 추출 실패: {}", e.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private String callOpenAi(String prompt, String schemaName, Map<String, Object> schema) {
        var request = new java.util.HashMap<String, Object>();
        request.put("model", model);
        request.put("messages", List.of(
                Map.of("role", "system", "content", "당신은 텍스트 분석 전문가입니다. 반드시 JSON만 출력하세요."),
                Map.of("role", "user", "content", prompt)
        ));
        request.put("response_format", Map.of(
                "type", "json_schema",
                "json_schema", Map.of(
                        "name", schemaName,
                        "strict", true,
                        "schema", schema
                )
        ));

        Map<String, Object> response = restClient.post()
                .uri("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(Map.class);

        if (response == null) throw new IllegalStateException("OpenAI 응답이 null입니다");
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        if (choices == null || choices.isEmpty()) throw new IllegalStateException("OpenAI 응답에 choices가 없습니다");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        if (message == null) throw new IllegalStateException("OpenAI 응답에 message가 없습니다");
        return (String) message.get("content");
    }

    private String smartTruncate(String text, int maxLength) {
        if (text.length() <= maxLength) return text;
        int third = maxLength / 3;
        String head = text.substring(0, third);
        String mid = text.substring((text.length() - third) / 2, (text.length() - third) / 2 + third);
        String tail = text.substring(text.length() - third);
        return head + "\n...(중략)...\n" + mid + "\n...(중략)...\n" + tail;
    }

    private record SubConceptItem(String label, String description) {}
    private record ConceptGroup(String main, List<SubConceptItem> subs) {}
}