package org.projeto.mockcrm.model;

public record LeadEvent(
        String leadId,
        String name,
        String role,
        String company,
        int employees,
        String segment,
        String source,
        String timestamp
) {}
