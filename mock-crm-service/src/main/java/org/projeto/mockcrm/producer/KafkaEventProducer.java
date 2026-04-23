package org.projeto.mockcrm.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.projeto.mockcrm.model.CrmUpdateEvent;
import org.projeto.mockcrm.model.InteractionEvent;
import org.projeto.mockcrm.model.LeadEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventProducer {

    private static final String LEAD_EVENTS        = "lead-events";
    private static final String INTERACTION_EVENTS = "interaction-events";
    private static final String CRM_UPDATES        = "crm-updates";

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void publishLeadEvent(LeadEvent e) {
        kafkaTemplate.send(LEAD_EVENTS, e.leadId(), toJson(e));
        log.info("[lead-events] {} — {} ({} @ {})", e.leadId(), e.name(), e.role(), e.company());
    }

    public void publishInteractionEvent(InteractionEvent e) {
        kafkaTemplate.send(INTERACTION_EVENTS, e.leadId(), toJson(e));
        log.debug("[interaction-events] {} — {} x{}", e.leadId(), e.type(), e.count());
    }

    public void publishCrmUpdate(CrmUpdateEvent e) {
        kafkaTemplate.send(CRM_UPDATES, e.leadId(), toJson(e));
        log.info("[crm-updates] {} -> {}", e.leadId(), e.stage());
    }

    private static String toJson(LeadEvent e) {
        return "{\"leadId\":\"" + e.leadId() + "\","
             + "\"name\":\"" + esc(e.name()) + "\","
             + "\"role\":\"" + esc(e.role()) + "\","
             + "\"company\":\"" + esc(e.company()) + "\","
             + "\"employees\":" + e.employees() + ","
             + "\"segment\":\"" + e.segment() + "\","
             + "\"source\":\"" + e.source() + "\","
             + "\"timestamp\":\"" + e.timestamp() + "\"}";
    }

    private static String toJson(InteractionEvent e) {
        return "{\"leadId\":\"" + e.leadId() + "\","
             + "\"type\":\"" + e.type() + "\","
             + "\"count\":" + e.count() + ","
             + "\"timestamp\":\"" + e.timestamp() + "\"}";
    }

    private static String toJson(CrmUpdateEvent e) {
        return "{\"leadId\":\"" + e.leadId() + "\","
             + "\"stage\":\"" + e.stage() + "\","
             + "\"timestamp\":\"" + e.timestamp() + "\"}";
    }

    private static String esc(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
