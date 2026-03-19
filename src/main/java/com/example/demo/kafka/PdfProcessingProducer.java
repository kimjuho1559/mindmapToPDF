package com.example.demo.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PdfProcessingProducer {

    private static final String TOPIC = "pdf-processing";

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void sendMindMapId(Long mindMapId) {
        log.info("Kafka 이벤트 발행 - mindMapId: {}", mindMapId);
        kafkaTemplate.send(TOPIC, mindMapId.toString());
    }
}
