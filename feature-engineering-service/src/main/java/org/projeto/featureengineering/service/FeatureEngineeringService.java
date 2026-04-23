package org.projeto.featureengineering.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.projeto.featureengineering.model.LeadFeatures;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeatureEngineeringService {

    private final RedisFeatureStore redisFeatureStore;

    public void processLeadEvent(Map<String, Object> data) {
        String leadId  = str(data, "leadId");
        String role    = str(data, "role");
        int employees  = num(data, "employees");
        String segment = str(data, "segment");
        String source  = str(data, "source");

        LeadFeatures features = LeadFeatures.builder()
                .leadId(leadId)
                .name(str(data, "name"))
                .company(str(data, "company"))
                .role(role)
                .employees(employees)
                .segment(segment)
                .source(source)
                .crmStage("NOVO")
                .roleScore(calcRoleScore(role))
                .companySizeScore(calcCompanySizeScore(employees))
                .segmentFitScore(calcSegmentFitScore(segment))
                .sourceScore(calcSourceScore(source))
                .emailOpenScore(0)
                .pricingPageVisits(0)
                .demoWatched(false)
                .lastContactEpoch(System.currentTimeMillis())
                .crmStageScore(10)
                .lastUpdated(Instant.now().toString())
                .build();

        redisFeatureStore.save(features);
        log.info("[lead-event] {} salvo no Redis — {} ({})", leadId, features.getName(), role);
    }

    public void processInteractionEvent(Map<String, Object> data) {
        String leadId = str(data, "leadId");
        String type   = str(data, "type");
        int count     = num(data, "count", 1);

        redisFeatureStore.find(leadId).ifPresentOrElse(f -> {
            switch (type) {
                case "EMAIL_OPEN"          -> f.setEmailOpenScore(Math.min(100, f.getEmailOpenScore() + count * 10));
                case "PRICING_PAGE_VISIT"  -> f.setPricingPageVisits(f.getPricingPageVisits() + count);
                case "DEMO_WATCHED"        -> f.setDemoWatched(true);
                default -> {} // SITE_VISIT — atualiza apenas lastContact
            }
            f.setLastContactEpoch(System.currentTimeMillis());
            f.setLastUpdated(Instant.now().toString());
            redisFeatureStore.save(f);
            log.info("[interaction] {} -> {} x{}", leadId, type, count);
        }, () -> log.warn("[interaction] Lead {} não encontrado no Redis", leadId));
    }

    public void processCrmUpdate(Map<String, Object> data) {
        String leadId = str(data, "leadId");
        String stage  = str(data, "stage");

        redisFeatureStore.find(leadId).ifPresentOrElse(f -> {
            f.setCrmStage(stage);
            f.setCrmStageScore(calcCrmStageScore(stage));
            f.setLastContactEpoch(System.currentTimeMillis());
            f.setLastUpdated(Instant.now().toString());
            redisFeatureStore.save(f);
            log.info("[crm-update] {} -> estágio {}", leadId, stage);
        }, () -> log.warn("[crm-update] Lead {} não encontrado no Redis", leadId));
    }

    // --- cálculo de features ---

    private int calcRoleScore(String role) {
        if (role == null) return 30;
        return switch (role) {
            case "CEO", "CTO"                          -> 95;
            case "VP de Vendas"                        -> 85;
            case "Diretor de TI", "Diretor Comercial"  -> 90;
            case "Gerente de TI", "Gerente Comercial"  -> 70;
            default                                    -> 30;
        };
    }

    private int calcCompanySizeScore(int employees) {
        if (employees < 50)   return 20;
        if (employees < 200)  return 50;
        if (employees < 1000) return 80;
        return 90;
    }

    private int calcSegmentFitScore(String segment) {
        if (segment == null) return 50;
        return switch (segment) {
            case "Tecnologia", "Financeiro"  -> 90;
            case "Manufatura", "Saúde"       -> 80;
            case "Varejo", "Serviços"        -> 60;
            default                          -> 50;
        };
    }

    private int calcSourceScore(String source) {
        if (source == null) return 50;
        return switch (source) {
            case "indicacao" -> 90;
            case "linkedin"  -> 70;
            case "evento"    -> 60;
            default          -> 50;
        };
    }

    private int calcCrmStageScore(String stage) {
        if (stage == null) return 10;
        return switch (stage) {
            case "NOVO"        -> 10;
            case "QUALIFICADO" -> 40;
            case "PROPOSTA"    -> 80;
            case "NEGOCIACAO"  -> 95;
            case "FECHADO"     -> 100;
            default            -> 10;
        };
    }

    // --- helpers ---

    private static String str(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v != null ? v.toString() : "";
    }

    private static int num(Map<String, Object> m, String key) {
        return num(m, key, 0);
    }

    private static int num(Map<String, Object> m, String key, int def) {
        Object v = m.get(key);
        return v instanceof Number n ? n.intValue() : def;
    }
}
