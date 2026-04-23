package org.projeto.mockcrm.model;

public record InteractionEvent(
        String leadId,
        String type,
        int count,
        String timestamp
) {}
