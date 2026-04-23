package org.projeto.featureengineering.service;

import lombok.RequiredArgsConstructor;
import org.projeto.featureengineering.model.LeadFeatures;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RedisFeatureStore {

    private static final String KEY_PREFIX = "lead:";

    private final StringRedisTemplate redisTemplate;
    private final org.springframework.boot.json.JsonParser jsonParser =
            JsonParserFactory.getJsonParser();

    public void save(LeadFeatures f) {
        redisTemplate.opsForValue().set(KEY_PREFIX + f.getLeadId(), toJson(f));
    }

    public Optional<LeadFeatures> find(String leadId) {
        String json = redisTemplate.opsForValue().get(KEY_PREFIX + leadId);
        if (json == null) return Optional.empty();
        return Optional.of(fromJson(json));
    }

    public Optional<String> findRaw(String leadId) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(KEY_PREFIX + leadId));
    }

    public Set<String> getAllKeys() {
        Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
        return keys != null ? keys : Set.of();
    }

    private LeadFeatures fromJson(String json) {
        Map<String, Object> m = jsonParser.parseMap(json);
        return LeadFeatures.builder()
                .leadId(str(m, "leadId"))
                .name(str(m, "name"))
                .company(str(m, "company"))
                .role(str(m, "role"))
                .employees(num(m, "employees"))
                .segment(str(m, "segment"))
                .source(str(m, "source"))
                .crmStage(str(m, "crmStage", "NOVO"))
                .roleScore(num(m, "roleScore"))
                .companySizeScore(num(m, "companySizeScore"))
                .segmentFitScore(num(m, "segmentFitScore"))
                .sourceScore(num(m, "sourceScore"))
                .emailOpenScore(num(m, "emailOpenScore"))
                .pricingPageVisits(num(m, "pricingPageVisits"))
                .demoWatched(Boolean.TRUE.equals(m.get("demoWatched")))
                .lastContactEpoch(((Number) m.getOrDefault("lastContactEpoch", 0L)).longValue())
                .crmStageScore(num(m, "crmStageScore", 10))
                .lastUpdated(str(m, "lastUpdated"))
                .build();
    }

    private static String toJson(LeadFeatures f) {
        return "{"
            + "\"leadId\":\"" + f.getLeadId() + "\","
            + "\"name\":\"" + esc(f.getName()) + "\","
            + "\"company\":\"" + esc(f.getCompany()) + "\","
            + "\"role\":\"" + esc(f.getRole()) + "\","
            + "\"employees\":" + f.getEmployees() + ","
            + "\"segment\":\"" + f.getSegment() + "\","
            + "\"source\":\"" + f.getSource() + "\","
            + "\"crmStage\":\"" + f.getCrmStage() + "\","
            + "\"roleScore\":" + f.getRoleScore() + ","
            + "\"companySizeScore\":" + f.getCompanySizeScore() + ","
            + "\"segmentFitScore\":" + f.getSegmentFitScore() + ","
            + "\"sourceScore\":" + f.getSourceScore() + ","
            + "\"emailOpenScore\":" + f.getEmailOpenScore() + ","
            + "\"pricingPageVisits\":" + f.getPricingPageVisits() + ","
            + "\"demoWatched\":" + f.isDemoWatched() + ","
            + "\"lastContactEpoch\":" + f.getLastContactEpoch() + ","
            + "\"crmStageScore\":" + f.getCrmStageScore() + ","
            + "\"lastUpdated\":\"" + f.getLastUpdated() + "\""
            + "}";
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String str(Map<String, Object> m, String key) {
        return str(m, key, "");
    }

    private static String str(Map<String, Object> m, String key, String def) {
        Object v = m.get(key);
        return v != null ? v.toString() : def;
    }

    private static int num(Map<String, Object> m, String key) {
        return num(m, key, 0);
    }

    private static int num(Map<String, Object> m, String key, int def) {
        Object v = m.get(key);
        return v instanceof Number n ? n.intValue() : def;
    }
}
