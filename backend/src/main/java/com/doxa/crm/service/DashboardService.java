package com.doxa.crm.service;

import com.doxa.crm.domain.entity.Opportunity;
import com.doxa.crm.domain.enums.OpportunityStatus;
import com.doxa.crm.dto.dashboard.DailySeriesPoint;
import com.doxa.crm.dto.dashboard.DashboardFilter;
import com.doxa.crm.dto.dashboard.DashboardKpisResponse;
import com.doxa.crm.dto.dashboard.FunnelStepResponse;
import com.doxa.crm.repository.OpportunityRepository;
import com.doxa.crm.repository.spec.CrmSpecifications;
import com.doxa.crm.security.AuthUser;
import com.doxa.crm.security.RolePolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class DashboardService {

    private static final ZoneId REPORT_ZONE = ZoneId.of("America/New_York");
    private static final DateTimeFormatter DAY_LABEL =
            DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH);
    private static final List<String> FORM_ONLY_STAGES = List.of("form-no-booking", "form-no-answer");

    private final OpportunityRepository opportunityRepository;
    private final BigDecimal defaultCostPerLead;

    public DashboardService(
            OpportunityRepository opportunityRepository,
            @Value("${app.dashboard.default-cost-per-lead:158}") BigDecimal defaultCostPerLead
    ) {
        this.opportunityRepository = opportunityRepository;
        this.defaultCostPerLead = defaultCostPerLead;
    }

    @Transactional(readOnly = true)
    public DashboardKpisResponse getKpis(AuthUser user, DashboardFilter filter) {
        long leads = count(user, filter, null);
        long triage = count(user, filter, CrmSpecifications.opportunityStageSlugNotIn(FORM_ONLY_STAGES));
        long scBooked = count(user, filter,
                CrmSpecifications.opportunityStageSlugIn(RolePolicy.SC_BOOKED_STAGE_SLUGS)
                        .or(CrmSpecifications.opportunityStatus(OpportunityStatus.WON)));
        long scShown = count(user, filter,
                CrmSpecifications.opportunityStageSlugIn(RolePolicy.SC_SHOWN_STAGE_SLUGS)
                        .or(CrmSpecifications.opportunityStatus(OpportunityStatus.WON)));
        long closes = count(user, filter, CrmSpecifications.opportunityStatus(OpportunityStatus.WON));

        BigDecimal adSpend = defaultCostPerLead.multiply(BigDecimal.valueOf(leads));
        BigDecimal cashCollected = sumWonValue(user, filter);

        return new DashboardKpisResponse(
                adSpend,
                leads,
                costPer(adSpend, leads),
                triage,
                costPer(adSpend, triage),
                scBooked,
                costPer(adSpend, scBooked),
                scShown,
                costPer(adSpend, scShown),
                closes,
                costPer(adSpend, closes),
                cashCollected,
                cashCollected,
                roas(cashCollected, adSpend),
                roas(cashCollected, adSpend)
        );
    }

    @Transactional(readOnly = true)
    public List<DailySeriesPoint> getDailySeries(AuthUser user, DashboardFilter filter) {
        LocalDate fromDate = filter.from().atZone(REPORT_ZONE).toLocalDate();
        LocalDate toDate = filter.to().atZone(REPORT_ZONE).toLocalDate();

        List<DailySeriesPoint> points = new ArrayList<>();
        for (LocalDate day = fromDate; !day.isAfter(toDate); day = day.plusDays(1)) {
            Instant dayStart = day.atStartOfDay(REPORT_ZONE).toInstant();
            Instant dayEnd = day.plusDays(1).atStartOfDay(REPORT_ZONE).toInstant().minusMillis(1);

            DashboardFilter dayFilter = new DashboardFilter(dayStart, dayEnd, filter.assignedUserId(), filter.adSet());
            long leads = count(user, new DashboardFilter(dayStart, dayEnd, filter.assignedUserId(), filter.adSet()), null);
            BigDecimal spend = defaultCostPerLead.multiply(BigDecimal.valueOf(leads));
            BigDecimal cashCollected = sumWonValue(user, dayFilter);

            points.add(new DailySeriesPoint(
                    day.format(DAY_LABEL),
                    spend,
                    cashCollected,
                    cashCollected
            ));
        }
        return points;
    }

    @Transactional(readOnly = true)
    public List<FunnelStepResponse> getFunnel(AuthUser user, DashboardFilter filter) {
        long leads = count(user, filter, null);
        long triage = count(user, filter, CrmSpecifications.opportunityStageSlugNotIn(FORM_ONLY_STAGES));
        long scBooked = count(user, filter,
                CrmSpecifications.opportunityStageSlugIn(RolePolicy.SC_BOOKED_STAGE_SLUGS)
                        .or(CrmSpecifications.opportunityStatus(OpportunityStatus.WON)));
        long scShown = count(user, filter,
                CrmSpecifications.opportunityStageSlugIn(RolePolicy.SC_SHOWN_STAGE_SLUGS)
                        .or(CrmSpecifications.opportunityStatus(OpportunityStatus.WON)));
        long closes = count(user, filter, CrmSpecifications.opportunityStatus(OpportunityStatus.WON));

        return List.of(
                step("Leads", leads, leads),
                step("Triage", triage, leads),
                step("SC Booked", scBooked, leads),
                step("SC Shown", scShown, leads),
                step("Closes", closes, leads)
        );
    }

    public DashboardFilter resolveFilter(
            AuthUser user,
            int periodDays,
            UUID assignedUserId,
            String adSet
    ) {
        Instant to = Instant.now();
        Instant from = to.minusSeconds((long) periodDays * 24 * 3600);
        UUID effectiveAssignedUser = RolePolicy.effectiveAssignedUserFilter(user, assignedUserId);
        return new DashboardFilter(from, to, effectiveAssignedUser, adSet);
    }

    private FunnelStepResponse step(String stage, long count, long leads) {
        int pct = leads == 0 ? 0 : (int) Math.round((count * 100.0) / leads);
        return new FunnelStepResponse(stage, pct, count);
    }

    private long count(AuthUser user, DashboardFilter filter, Specification<Opportunity> extra) {
        Specification<Opportunity> spec = baseSpec(user, filter);
        if (extra != null) {
            spec = spec.and(extra);
        }
        return opportunityRepository.count(spec);
    }

    private BigDecimal sumWonValue(AuthUser user, DashboardFilter filter) {
        Specification<Opportunity> spec = Specification
                .where(CrmSpecifications.opportunityBelongsToLicense(user.getLicenseId()))
                .and(CrmSpecifications.opportunityMatchesRole(user))
                .and(CrmSpecifications.opportunityUpdatedBetween(filter.from(), filter.to()))
                .and(CrmSpecifications.opportunityAssignedTo(filter.assignedUserId()))
                .and(CrmSpecifications.opportunityAdSet(filter.adSet()))
                .and(CrmSpecifications.opportunityStatus(OpportunityStatus.WON));

        return opportunityRepository.findAll(spec).stream()
                .map(Opportunity::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Specification<Opportunity> baseSpec(AuthUser user, DashboardFilter filter) {
        return Specification.where(CrmSpecifications.opportunityBelongsToLicense(user.getLicenseId()))
                .and(CrmSpecifications.opportunityMatchesRole(user))
                .and(CrmSpecifications.opportunityCreatedBetween(filter.from(), filter.to()))
                .and(CrmSpecifications.opportunityAssignedTo(filter.assignedUserId()))
                .and(CrmSpecifications.opportunityAdSet(filter.adSet()));
    }

    private BigDecimal costPer(BigDecimal spend, long count) {
        if (count == 0) {
            return BigDecimal.ZERO;
        }
        return spend.divide(BigDecimal.valueOf(count), 0, RoundingMode.HALF_UP);
    }

    private BigDecimal roas(BigDecimal cashCollected, BigDecimal adSpend) {
        if (adSpend.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return cashCollected.divide(adSpend, 2, RoundingMode.HALF_UP);
    }
}
