package org.projeto.featureengineering.controller;

import lombok.RequiredArgsConstructor;
import org.projeto.featureengineering.service.RedisFeatureStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FeaturesController {

    private final RedisFeatureStore redisFeatureStore;

    @GetMapping("/features/{leadId}")
    public ResponseEntity<String> getFeatures(@PathVariable String leadId) {
        return redisFeatureStore.findRaw(leadId)
                .map(json -> ResponseEntity.ok()
                        .header("Content-Type", "application/json")
                        .body(json))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/features")
    public Set<String> getAllKeys() {
        return redisFeatureStore.getAllKeys();
    }
}
