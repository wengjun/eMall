package com.emall.cost.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.emall.common.id.SnowflakeIdGenerator;
import com.emall.cost.domain.CostActionStatus;
import com.emall.cost.domain.CostActionType;
import com.emall.cost.domain.CostBudget;
import com.emall.cost.domain.CostOptimizationAction;
import com.emall.cost.domain.CostSignalType;
import com.emall.cost.domain.CostSummary;
import com.emall.cost.repository.InMemoryCostRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class CostGovernanceServiceTest {
    private final InMemoryCostRepository repository = new InMemoryCostRepository();
    private final CostGovernanceService service = new CostGovernanceService(repository, new SnowflakeIdGenerator(17L));

    @Test
    void opensActionWhenCacheHitRatioFallsBelowThreshold() {
        service.recordSignal("search", CostSignalType.CACHE_HIT_RATIO, new BigDecimal("0.72"),
                new BigDecimal("0.85"), Instant.now());
        service.recordSignal("search", CostSignalType.CACHE_HIT_RATIO, new BigDecimal("0.70"),
                new BigDecimal("0.85"), Instant.now());

        List<CostOptimizationAction> actions = service.findActiveActions("search");

        assertThat(actions).hasSize(1);
        assertThat(actions.get(0).actionType()).isEqualTo(CostActionType.INCREASE_CACHE_TTL);
        assertThat(actions.get(0).priority()).isEqualTo(1);
    }

    @Test
    void opensActionWhenIndexStorageExceedsThreshold() {
        service.recordSignal("search", CostSignalType.INDEX_STORAGE_GB, new BigDecimal("650"),
                new BigDecimal("500"), Instant.now());

        List<CostOptimizationAction> actions = service.findActiveActions("search");

        assertThat(actions).hasSize(1);
        assertThat(actions.get(0).actionType()).isEqualTo(CostActionType.ROLLOVER_INDEX);
    }

    @Test
    void budgetSummaryFlagsAlertThreshold() {
        CostBudget budget = service.upsertBudget("export", new BigDecimal("10000"), new BigDecimal("8500"),
                "usd", 80, true);

        CostSummary summary = service.summary("export");

        assertThat(budget.currency()).isEqualTo("USD");
        assertThat(summary.budgetAlert()).isTrue();
        assertThat(summary.monthlyBudget()).isEqualByComparingTo("10000.000000");
    }

    @Test
    void actionStatusCanBeChanged() {
        service.recordSignal("fulfillment", CostSignalType.HOT_STORAGE_GB, new BigDecimal("1200"),
                new BigDecimal("1000"), Instant.now());
        long actionId = service.findActiveActions("fulfillment").get(0).actionId();

        CostOptimizationAction completed = service.changeActionStatus(actionId, CostActionStatus.COMPLETED);

        assertThat(completed.status()).isEqualTo(CostActionStatus.COMPLETED);
        assertThat(service.findActiveActions("fulfillment")).isEmpty();
    }
}
