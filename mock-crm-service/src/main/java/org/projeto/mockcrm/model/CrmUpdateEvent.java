package org.projeto.mockcrm.model;

public record CrmUpdateEvent(
        String leadId,
        String stage,
        String timestamp
) {}
