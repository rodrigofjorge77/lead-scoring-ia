package org.projeto.mockcrm.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Lead {
    private String leadId;
    private String name;
    private String role;
    private String company;
    private int employees;
    private String segment;
    private String source;
    private String crmStage;
    private LocalDateTime createdAt;
    private LocalDateTime lastInteraction;
}
