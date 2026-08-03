package com.doxa.crm.config;

import com.doxa.crm.domain.entity.Contact;
import com.doxa.crm.domain.entity.License;
import com.doxa.crm.domain.entity.Opportunity;
import com.doxa.crm.domain.entity.Pipeline;
import com.doxa.crm.domain.entity.Stage;
import com.doxa.crm.domain.entity.User;
import com.doxa.crm.domain.enums.LicenseStatus;
import com.doxa.crm.domain.enums.OpportunitySource;
import com.doxa.crm.domain.enums.OpportunityStatus;
import com.doxa.crm.domain.enums.UserRole;
import com.doxa.crm.repository.ContactRepository;
import com.doxa.crm.repository.LicenseRepository;
import com.doxa.crm.repository.OpportunityRepository;
import com.doxa.crm.repository.PipelineRepository;
import com.doxa.crm.repository.StageRepository;
import com.doxa.crm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.UUID;

@TestConfiguration
@RequiredArgsConstructor
public class TestDataConfig {

    public static final UUID LICENSE_DEMO_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final UUID LICENSE_OTHER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Bean
    TestDataSeeder testDataSeeder(
            LicenseRepository licenseRepository,
            UserRepository userRepository,
            PipelineRepository pipelineRepository,
            StageRepository stageRepository,
            ContactRepository contactRepository,
            OpportunityRepository opportunityRepository,
            PasswordEncoder passwordEncoder
    ) {
        return new TestDataSeeder(
                licenseRepository,
                userRepository,
                pipelineRepository,
                stageRepository,
                contactRepository,
                opportunityRepository,
                passwordEncoder
        );
    }

    @RequiredArgsConstructor
    static class TestDataSeeder {
        private final LicenseRepository licenseRepository;
        private final UserRepository userRepository;
        private final PipelineRepository pipelineRepository;
        private final StageRepository stageRepository;
        private final ContactRepository contactRepository;
        private final OpportunityRepository opportunityRepository;
        private final PasswordEncoder passwordEncoder;

        @jakarta.annotation.PostConstruct
        void seed() {
            if (licenseRepository.count() > 0) {
                return;
            }

            License demo = licenseRepository.save(License.builder()
                    .id(LICENSE_DEMO_ID)
                    .companyName("Demo")
                    .status(LicenseStatus.ACTIVE)
                    .webhookSecret("secret-demo")
                    .build());

            License other = licenseRepository.save(License.builder()
                    .id(LICENSE_OTHER_ID)
                    .companyName("Other")
                    .status(LicenseStatus.ACTIVE)
                    .webhookSecret("secret-other")
                    .build());

            userRepository.save(User.builder()
                    .id(UUID.fromString("33333333-3333-3333-3333-333333333333"))
                    .license(demo)
                    .email("admin@demo.doxa.com")
                    .passwordHash(passwordEncoder.encode("password123"))
                    .role(UserRole.ADMIN)
                    .build());

            userRepository.save(User.builder()
                    .id(UUID.fromString("44444444-4444-4444-4444-444444444444"))
                    .license(demo)
                    .email("closer@demo.doxa.com")
                    .passwordHash(passwordEncoder.encode("password123"))
                    .role(UserRole.CLOSER)
                    .build());

            Pipeline demoPipeline = pipelineRepository.save(Pipeline.builder()
                    .id(UUID.fromString("66666666-6666-6666-6666-666666666666"))
                    .license(demo)
                    .name("Ads Pipeline")
                    .build());

            Stage demoStage = stageRepository.save(Stage.builder()
                    .id(UUID.fromString("aaaa0001-0001-0001-0001-000000000001"))
                    .pipeline(demoPipeline)
                    .slug("new-lead")
                    .name("New Lead")
                    .position(0)
                    .monetaryValue(BigDecimal.ZERO)
                    .build());

            Contact demoContact = contactRepository.save(Contact.builder()
                    .id(UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"))
                    .license(demo)
                    .name("Demo Contact")
                    .email("demo@example.com")
                    .dedupeKey("email:demo@example.com")
                    .build());

            opportunityRepository.save(Opportunity.builder()
                    .id(UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd"))
                    .license(demo)
                    .contact(demoContact)
                    .stage(demoStage)
                    .source(OpportunitySource.MANUAL)
                    .status(OpportunityStatus.OPEN)
                    .build());

            Pipeline otherPipeline = pipelineRepository.save(Pipeline.builder()
                    .id(UUID.fromString("88888888-8888-8888-8888-888888888888"))
                    .license(other)
                    .name("Ads Pipeline")
                    .build());

            Stage otherStage = stageRepository.save(Stage.builder()
                    .id(UUID.fromString("99999999-9999-9999-9999-999999999999"))
                    .pipeline(otherPipeline)
                    .slug("new-lead")
                    .name("New Lead")
                    .position(0)
                    .monetaryValue(BigDecimal.ZERO)
                    .build());

            Contact otherContact = contactRepository.save(Contact.builder()
                    .id(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"))
                    .license(other)
                    .name("Other Tenant Contact")
                    .email("secret@other-tenant.com")
                    .dedupeKey("email:secret@other-tenant.com")
                    .build());

            opportunityRepository.save(Opportunity.builder()
                    .id(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"))
                    .license(other)
                    .contact(otherContact)
                    .stage(otherStage)
                    .source(OpportunitySource.MANUAL)
                    .status(OpportunityStatus.OPEN)
                    .build());
        }
    }
}
