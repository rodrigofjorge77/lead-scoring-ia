package org.projeto.featureengineering.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.projeto.featureengineering.service.FeatureEngineeringService;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CrmUpdateConsumer {

    private final FeatureEngineeringService service;
    private final org.springframework.boot.json.JsonParser parser = JsonParserFactory.getJsonParser();

    @KafkaListener(topics = "crm-updates", groupId = "feature-engineering",
                   containerFactory = "kafkaListenerContainerFactory")
    public void consume(String message) {
        try {
            Map<String, Object> data = parser.parseMap(message);
            service.processCrmUpdate(data);
        } catch (Exception e) {
            log.error("Erro ao processar crm-update: {} — msg: {}", e.getMessage(), message);
        }
    }
}
