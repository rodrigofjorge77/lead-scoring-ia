package org.projeto.featureengineering.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeadFeatures {
    private String leadId;
    private String name;
    private String company;
    private String role;
    private int employees;
    private String segment;
    private String source;
    private String crmStage;

    // features calculadas
    private int roleScore;
    private int companySizeScore;
    private int segmentFitScore;
    private int sourceScore;
    private int emailOpenScore;
    private int pricingPageVisits;
    private boolean demoWatched;
    private long lastContactEpoch;
    private int crmStageScore;

    private String lastUpdated;
}
