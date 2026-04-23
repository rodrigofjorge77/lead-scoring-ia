package org.projeto.mockcrm.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.projeto.mockcrm.model.CrmUpdateEvent;
import org.projeto.mockcrm.model.InteractionEvent;
import org.projeto.mockcrm.model.Lead;
import org.projeto.mockcrm.model.LeadEvent;
import org.projeto.mockcrm.producer.KafkaEventProducer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class MockCrmService {

    private static final String[] ROLES = {
        "CEO", "CTO", "Diretor de TI", "Diretor Comercial", "VP de Vendas",
        "Gerente de TI", "Gerente Comercial", "Analista de TI", "Analista Comercial"
    };
    private static final String[] SEGMENTS = {
        "Manufatura", "Varejo", "Financeiro", "Saúde", "Educação", "Tecnologia", "Serviços"
    };
    private static final String[] SOURCES = { "site", "linkedin", "evento", "indicacao" };
    private static final String[] COMPANIES = {
        "TechSolve", "DataBridge", "InnovateCorp", "DigitalFlow", "SmartBiz",
        "CloudVenture", "NextGen", "ProSystems", "FutureTech", "AlphaCorp"
    };
    private static final String[] FIRST_NAMES = {
        "João", "Maria", "Carlos", "Ana", "Pedro", "Lúcia", "Rafael", "Júlia", "Fernando", "Patrícia"
    };
    private static final String[] LAST_NAMES = {
        "Silva", "Santos", "Oliveira", "Souza", "Lima", "Costa", "Ferreira", "Rodrigues", "Alves", "Pereira"
    };
    private static final String[] INTERACTION_TYPES = {
        "EMAIL_OPEN", "PRICING_PAGE_VISIT", "DEMO_WATCHED", "SITE_VISIT"
    };
    private static final String[] CRM_STAGES = { "NOVO", "QUALIFICADO", "PROPOSTA", "NEGOCIACAO", "FECHADO" };
    private static final int[][] EMPLOYEE_RANGES = { {10, 49}, {50, 200}, {201, 1000}, {1001, 5000} };

    private final Map<String, Lead> leads = new ConcurrentHashMap<>();
    private final AtomicInteger counter = new AtomicInteger(1);
    private final KafkaEventProducer producer;
    private final Random random = new Random();

    @Value("${mock.crm.initial-leads:10}")
    private int initialLeads;

    @PostConstruct
    public void init() {
        log.info("Criando {} leads iniciais...", initialLeads);
        for (int i = 0; i < initialLeads; i++) {
            createAndPublishLead();
        }
    }

    public Lead createAndPublishLead() {
        String leadId = String.format("L-%03d", counter.getAndIncrement());
        int[] range = EMPLOYEE_RANGES[random.nextInt(EMPLOYEE_RANGES.length)];

        Lead lead = Lead.builder()
                .leadId(leadId)
                .name(randomName())
                .role(ROLES[random.nextInt(ROLES.length)])
                .company(COMPANIES[random.nextInt(COMPANIES.length)] + " " + (random.nextInt(900) + 100))
                .employees(range[0] + random.nextInt(range[1] - range[0] + 1))
                .segment(SEGMENTS[random.nextInt(SEGMENTS.length)])
                .source(SOURCES[random.nextInt(SOURCES.length)])
                .crmStage("NOVO")
                .createdAt(LocalDateTime.now())
                .lastInteraction(LocalDateTime.now())
                .build();

        leads.put(leadId, lead);

        producer.publishLeadEvent(new LeadEvent(
                lead.getLeadId(), lead.getName(), lead.getRole(),
                lead.getCompany(), lead.getEmployees(),
                lead.getSegment(), lead.getSource(),
                Instant.now().toString()
        ));

        return lead;
    }

    @Scheduled(fixedDelayString = "${mock.crm.interaction-interval:15000}")
    public void simulateInteractions() {
        if (leads.isEmpty()) return;
        List<Lead> list = new ArrayList<>(leads.values());
        int count = 1 + random.nextInt(Math.min(3, list.size()));
        for (int i = 0; i < count; i++) {
            generateInteractionForLead(list.get(random.nextInt(list.size())).getLeadId());
        }
    }

    @Scheduled(fixedDelayString = "${mock.crm.stage-advance-interval:60000}")
    public void advanceLeadStages() {
        leads.values().stream()
                .filter(l -> !"FECHADO".equals(l.getCrmStage()))
                .filter(l -> random.nextDouble() < 0.15)
                .forEach(l -> {
                    String next = nextStage(l.getCrmStage());
                    l.setCrmStage(next);
                    l.setLastInteraction(LocalDateTime.now());
                    producer.publishCrmUpdate(new CrmUpdateEvent(l.getLeadId(), next, Instant.now().toString()));
                });
    }

    public void generateInteractionForLead(String leadId) {
        Lead lead = leads.get(leadId);
        if (lead == null) return;
        String type = INTERACTION_TYPES[random.nextInt(INTERACTION_TYPES.length)];
        int count = (type.equals("EMAIL_OPEN") || type.equals("SITE_VISIT")) ? random.nextInt(3) + 1 : 1;
        lead.setLastInteraction(LocalDateTime.now());
        producer.publishInteractionEvent(new InteractionEvent(leadId, type, count, Instant.now().toString()));
    }

    public Collection<Lead> getAllLeads() {
        return leads.values();
    }

    public Lead getLead(String leadId) {
        return leads.get(leadId);
    }

    private String nextStage(String current) {
        return switch (current) {
            case "NOVO"        -> "QUALIFICADO";
            case "QUALIFICADO" -> "PROPOSTA";
            case "PROPOSTA"    -> "NEGOCIACAO";
            case "NEGOCIACAO"  -> "FECHADO";
            default            -> current;
        };
    }

    private String randomName() {
        return FIRST_NAMES[random.nextInt(FIRST_NAMES.length)] + " " + LAST_NAMES[random.nextInt(LAST_NAMES.length)];
    }
}
