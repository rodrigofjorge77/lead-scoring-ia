package org.projeto.mockcrm.controller;

import lombok.RequiredArgsConstructor;
import org.projeto.mockcrm.model.Lead;
import org.projeto.mockcrm.service.MockCrmService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CrmController {

    private final MockCrmService mockCrmService;

    @GetMapping("/leads")
    public Collection<Lead> getAllLeads() {
        return mockCrmService.getAllLeads();
    }

    @GetMapping("/leads/{leadId}")
    public ResponseEntity<Lead> getLead(@PathVariable String leadId) {
        Lead lead = mockCrmService.getLead(leadId);
        return lead != null ? ResponseEntity.ok(lead) : ResponseEntity.notFound().build();
    }

    @PostMapping("/leads")
    @ResponseStatus(HttpStatus.CREATED)
    public Lead createLead() {
        return mockCrmService.createAndPublishLead();
    }

    @PostMapping("/leads/{leadId}/interaction")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void generateInteraction(@PathVariable String leadId) {
        mockCrmService.generateInteractionForLead(leadId);
    }

    @PostMapping("/simulate/leads/{count}")
    @ResponseStatus(HttpStatus.CREATED)
    public String simulateBatch(@PathVariable int count) {
        for (int i = 0; i < count; i++) {
            mockCrmService.createAndPublishLead();
        }
        return "Criados " + count + " leads";
    }
}
