package com.doxa.crm.config;

import com.doxa.crm.domain.entity.Contact;
import com.doxa.crm.domain.entity.License;
import com.doxa.crm.domain.entity.Opportunity;
import com.doxa.crm.domain.entity.Pipeline;
import com.doxa.crm.domain.entity.Stage;
import com.doxa.crm.domain.entity.StageHistory;
import com.doxa.crm.domain.entity.User;
import com.doxa.crm.domain.enums.LicenseStatus;
import com.doxa.crm.domain.enums.OpportunitySource;
import com.doxa.crm.domain.enums.OpportunityStatus;
import com.doxa.crm.domain.enums.UserRole;
import com.doxa.crm.repository.ContactRepository;
import com.doxa.crm.repository.LicenseRepository;
import com.doxa.crm.repository.OpportunityRepository;
import com.doxa.crm.repository.PipelineRepository;
import com.doxa.crm.repository.StageHistoryRepository;
import com.doxa.crm.repository.StageRepository;
import com.doxa.crm.repository.UserRepository;
import com.doxa.crm.util.PhoneNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseSeeder implements ApplicationRunner {

    private static final UUID LICENSE_DEMO_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID LICENSE_OTHER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID USER_ADMIN_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID USER_CLOSER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID USER_SDR_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID PIPELINE_ADS_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");

    private static final String DEMO_PASSWORD = "password123";

    private final LicenseRepository licenseRepository;
    private final UserRepository userRepository;
    private final PipelineRepository pipelineRepository;
    private final StageRepository stageRepository;
    private final ContactRepository contactRepository;
    private final OpportunityRepository opportunityRepository;
    private final StageHistoryRepository stageHistoryRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.enabled:true}")
    private boolean seedEnabled;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!seedEnabled || licenseRepository.count() > 0) {
            return;
        }

        log.info("Seeding database with demo CRM data...");

        License demoLicense = seedDemoLicense();
        User admin = seedUser(USER_ADMIN_ID, demoLicense, "admin@demo.doxa.com", UserRole.ADMIN);
        User closer = seedUser(USER_CLOSER_ID, demoLicense, "closer@demo.doxa.com", UserRole.CLOSER);
        seedUser(USER_SDR_ID, demoLicense, "sdr@demo.doxa.com", UserRole.SDR);

        License otherLicense = seedOtherLicense();
        seedUser(UUID.fromString("77777777-7777-7777-7777-777777777777"), otherLicense,
                "admin@other.doxa.com", UserRole.ADMIN);

        Pipeline pipeline = seedAdsPipeline(demoLicense);
        Map<String, Stage> stages = seedStages(pipeline);
        List<OpportunitySeed> seeds = buildOpportunitySeeds(stages);

        List<Contact> contacts = new ArrayList<>();
        List<Opportunity> opportunities = new ArrayList<>();

        for (int i = 0; i < seeds.size(); i++) {
            OpportunitySeed seed = seeds.get(i);
            String phoneE164 = PhoneNormalizer.toE164(seed.phone());
            Contact contact = Contact.builder()
                    .id(UUID.randomUUID())
                    .license(demoLicense)
                    .name(seed.name())
                    .email(seed.email())
                    .phone(seed.phone())
                    .phoneE164(phoneE164)
                    .tags(seed.tags())
                    .dedupeKey(PhoneNormalizer.dedupeKey(seed.email(), phoneE164))
                    .build();
            contacts.add(contact);

            Opportunity opportunity = Opportunity.builder()
                    .id(UUID.randomUUID())
                    .license(demoLicense)
                    .contact(contact)
                    .stage(seed.stage())
                    .value(seed.value())
                    .adSet(seed.adSet())
                    .revenueMonthly(seed.revenueMonthly())
                    .source(OpportunitySource.MANUAL)
                    .assignedUser(i % 3 == 0 ? closer : (i % 3 == 1 ? admin : null))
                    .status(OpportunityStatus.OPEN)
                    .createdAt(seed.createdAt())
                    .updatedAt(seed.createdAt())
                    .build();
            opportunities.add(opportunity);
        }

        contactRepository.saveAll(contacts);
        opportunityRepository.saveAll(opportunities);

        for (Opportunity opportunity : opportunities) {
            stageHistoryRepository.save(StageHistory.builder()
                    .id(UUID.randomUUID())
                    .opportunity(opportunity)
                    .fromStage(null)
                    .toStage(opportunity.getStage())
                    .changedAt(opportunity.getCreatedAt())
                    .changedBy(admin)
                    .build());
        }

        log.info("Seed complete: {} contacts, {} opportunities", contacts.size(), opportunities.size());
    }

    private License seedDemoLicense() {
        return licenseRepository.save(License.builder()
                .id(LICENSE_DEMO_ID)
                .companyName("DOXA Demo License")
                .status(LicenseStatus.ACTIVE)
                .webhookSecret("whsec_demo_license_secret_change_me")
                .plan("pro")
                .build());
    }

    private License seedOtherLicense() {
        return licenseRepository.save(License.builder()
                .id(LICENSE_OTHER_ID)
                .companyName("Other Tenant (isolation test)")
                .status(LicenseStatus.ACTIVE)
                .webhookSecret("whsec_other_license_secret")
                .plan("starter")
                .build());
    }

    private User seedUser(UUID id, License license, String email, UserRole role) {
        return userRepository.save(User.builder()
                .id(id)
                .license(license)
                .email(email)
                .passwordHash(passwordEncoder.encode(DEMO_PASSWORD))
                .role(role)
                .build());
    }

    private Pipeline seedAdsPipeline(License license) {
        return pipelineRepository.save(Pipeline.builder()
                .id(PIPELINE_ADS_ID)
                .license(license)
                .name("Ads Pipeline")
                .build());
    }

    private Map<String, Stage> seedStages(Pipeline pipeline) {
        record StageDef(String slug, String name, int position, BigDecimal monetaryValue) {}

        List<StageDef> defs = List.of(
                new StageDef("form-no-booking", "Form Filled | No Booking", 0, BigDecimal.ZERO),
                new StageDef("form-no-answer", "Form Filled | No Answer", 1, BigDecimal.ZERO),
                new StageDef("new-lead", "New Lead", 2, BigDecimal.ZERO),
                new StageDef("early-triage", "Early Triage – No Answer", 3, BigDecimal.ZERO),
                new StageDef("waiting-reply", "#1 Waiting for Reply", 4, BigDecimal.ZERO),
                new StageDef("triage-not-qualified", "Triage Completed | Not Qualified", 5, BigDecimal.ZERO),
                new StageDef("triage-no-show", "Triage No Show", 6, BigDecimal.ZERO),
                new StageDef("qualified", "Qualified for Sales Call", 7, BigDecimal.ZERO),
                new StageDef("reconnect-1", "Reconnect #1", 8, new BigDecimal("210000")),
                new StageDef("reconnect-2", "Reconnect #2", 9, BigDecimal.ZERO)
        );

        Map<String, Stage> stages = new LinkedHashMap<>();
        for (StageDef def : defs) {
            Stage stage = Stage.builder()
                    .id(UUID.nameUUIDFromBytes(("stage-" + def.slug).getBytes()))
                    .pipeline(pipeline)
                    .slug(def.slug)
                    .name(def.name)
                    .position(def.position)
                    .monetaryValue(def.monetaryValue)
                    .build();
            stages.put(def.slug, stageRepository.save(stage));
        }
        return stages;
    }

    private List<OpportunitySeed> buildOpportunitySeeds(Map<String, Stage> stages) {
        Map<String, Integer> stageCounts = Map.ofEntries(
                Map.entry("form-no-booking", 8),
                Map.entry("form-no-answer", 6),
                Map.entry("new-lead", 1),
                Map.entry("early-triage", 0),
                Map.entry("waiting-reply", 0),
                Map.entry("triage-not-qualified", 9),
                Map.entry("triage-no-show", 9),
                Map.entry("qualified", 1),
                Map.entry("reconnect-1", 7),
                Map.entry("reconnect-2", 5)
        );

        String[] adSets = {
                "P2 | Broad | US & CAN | RE #1",
                "P2 | Broad | US & CAN | RE #2",
                "P2 | Broad | US & CAN | Doctors #1",
                "P2 | Broad | CA(US) | Doctors #2",
                "AUTO - Broad [Hannah]",
                "AUTO - Broad",
                "paid"
        };

        String[] revenues = {
                "Less than $5k/mo",
                "$5k - $10k/mo",
                "$10k - $25k/mo",
                "$25k - $50k/mo",
                "$50k+/mo"
        };

        String[] firstNames = {
                "Kumar", "Duncan", "Dina", "Shahrokh", "Alexandra", "Faris", "Crystal",
                "Samuel", "Miguel", "Az", "Benjie", "Tatanisha", "Islam", "Amna", "Kadim",
                "Theresa", "Moises", "Joe", "Charles", "Bruno", "Nada", "Jay", "Neil",
                "Hannah", "Marcus", "Priya", "Diego", "Fatima", "Oliver", "Sofia", "Andre",
                "Keisha", "Tomás", "Ingrid", "Rashid", "Elena", "Victor", "Amara", "Felix",
                "Yuki", "Omar", "Bianca", "Trevor", "Leila", "Gustavo", "Wei", "Nadia",
                "Pedro", "Chloe", "Ibrahim"
        };

        String[] lastNames = {
                "Kumar", "Reyes", "Lebowitz", "Poormehr", "Quinones", "Abusharif",
                "Broussard", "Success", "Leite", "Az", "Benas", "Funches", "Bashir",
                "Khan", "Ahmed", "Le", "Issa", "Bradley", "Dow", "Caraponale", "Noorzay",
                "Mehta", "Regal", "Silva", "Thompson", "Patel", "Martinez", "Hassan",
                "Brooks", "Costa", "Williams", "Johnson", "Ferreira", "Larsen", "Ali",
                "Petrova", "Nguyen", "Okafor", "Weber", "Tanaka", "Farouk", "Rossi",
                "Mitchell", "Haddad", "Barbosa", "Chen", "Karimi", "Almeida", "Dubois",
                "Mansour"
        };

        String[] tagPool = {"ads-lead", "triage-done", "qualified", "no-show", "reconnect", "hot"};

        List<OpportunitySeed> seeds = new ArrayList<>();
        int i = 0;

        for (Map.Entry<String, Integer> entry : stageCounts.entrySet()) {
            Stage stage = stages.get(entry.getKey());
            int count = entry.getValue();
            for (int j = 0; j < count; j++) {
                String fn = firstNames[i % firstNames.length];
                String ln = lastNames[(i * 3 + 7) % lastNames.length];
                String name = fn + " " + ln;
                String email = fn.toLowerCase() + "." + ln.toLowerCase().replaceAll("[^a-z]", "") + "@example.com";
                String phone = String.format("+1 (407) 555-0%02d%d", (i * 37) % 100, i % 10);

                BigDecimal value = switch (entry.getKey()) {
                    case "reconnect-1" -> new BigDecimal("30000");
                    case "qualified" -> new BigDecimal("15000");
                    default -> BigDecimal.ZERO;
                };

                List<String> tags = new ArrayList<>();
                tags.add(tagPool[i % tagPool.length]);
                if (i % 3 == 0) {
                    tags.add(tagPool[(i + 2) % tagPool.length]);
                }

                seeds.add(new OpportunitySeed(
                        name,
                        email,
                        phone,
                        stage,
                        i % 3 != 1 ? adSets[i % adSets.length] : null,
                        i % 4 != 2 ? revenues[(i * 2) % revenues.length] : null,
                        value,
                        tags,
                        makeCreatedAt(i + 3)
                ));
                i++;
            }
        }

        return seeds;
    }

    private Instant makeCreatedAt(int seed) {
        int month = 4 + (seed % 3);
        int day = (seed * 7) % 28 + 1;
        int hour = (seed * 5) % 12 + 1;
        int minute = (seed * 13) % 60;
        ZonedDateTime zdt = ZonedDateTime.of(2026, month, day, hour, minute, 0, 0, ZoneOffset.ofHours(-4));
        return zdt.toInstant();
    }

    private record OpportunitySeed(
            String name,
            String email,
            String phone,
            Stage stage,
            String adSet,
            String revenueMonthly,
            BigDecimal value,
            List<String> tags,
            Instant createdAt
    ) {
    }
}
