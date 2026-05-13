package com.emall.reliability;

import static com.emall.common.persistence.RowMaps.booleanValue;
import static com.emall.common.persistence.RowMaps.decimalValue;
import static com.emall.common.persistence.RowMaps.instantValue;
import static com.emall.common.persistence.RowMaps.intValue;
import static com.emall.common.persistence.RowMaps.longValue;
import static com.emall.common.persistence.RowMaps.stringValue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
class MybatisPlusReliabilityRepository implements ReliabilityRepository {
    private final ReliabilityMapper reliabilityMapper;

    MybatisPlusReliabilityRepository(ReliabilityMapper reliabilityMapper) {
        this.reliabilityMapper = reliabilityMapper;
    }

    @Override
    public CapacityRehearsal saveRehearsal(CapacityRehearsal rehearsal) {
        reliabilityMapper.saveRehearsal(rehearsal);
        return rehearsal;
    }

    @Override
    public Optional<CapacityRehearsal> findRehearsal(long rehearsalId) {
        return Optional.ofNullable(reliabilityMapper.findRehearsal(rehearsalId)).map(this::mapRehearsal);
    }

    @Override
    public List<CapacityRehearsal> findRehearsals() {
        return reliabilityMapper.findRehearsals().stream().map(this::mapRehearsal).toList();
    }

    @Override
    public SloObjective saveSlo(SloObjective slo) {
        reliabilityMapper.saveSlo(slo);
        return slo;
    }

    @Override
    public List<SloObjective> findSlos() {
        return reliabilityMapper.findSlos().stream().map(this::mapSlo).toList();
    }

    @Override
    public ChaosSchedule saveChaos(ChaosSchedule chaos) {
        reliabilityMapper.saveChaos(chaos);
        return chaos;
    }

    @Override
    public Optional<ChaosSchedule> findChaos(long chaosId) {
        return Optional.ofNullable(reliabilityMapper.findChaos(chaosId)).map(this::mapChaos);
    }

    @Override
    public List<ChaosSchedule> findChaosSchedules() {
        return reliabilityMapper.findChaosSchedules().stream().map(this::mapChaos).toList();
    }

    @Override
    public ReadinessGate saveReadinessGate(ReadinessGate gate) {
        reliabilityMapper.saveReadinessGate(gate);
        return gate;
    }

    @Override
    public List<ReadinessGate> findReadinessGates() {
        return reliabilityMapper.findReadinessGates().stream().map(this::mapGate).toList();
    }

    private CapacityRehearsal mapRehearsal(Map<String, Object> row) {
        return new CapacityRehearsal(longValue(row, "rehearsal_id"), stringValue(row, "service_name"),
                intValue(row, "target_qps"), intValue(row, "peak_concurrency"),
                GateStatus.valueOf(stringValue(row, "status")), instantValue(row, "created_at"),
                instantValue(row, "updated_at"));
    }

    private SloObjective mapSlo(Map<String, Object> row) {
        return new SloObjective(longValue(row, "slo_id"), stringValue(row, "service_name"),
                decimalValue(row, "availability_target"), intValue(row, "latency_p95_ms"),
                decimalValue(row, "error_budget_percent"), instantValue(row, "created_at"));
    }

    private ChaosSchedule mapChaos(Map<String, Object> row) {
        return new ChaosSchedule(longValue(row, "chaos_id"), stringValue(row, "service_name"),
                stringValue(row, "drill_type"), intValue(row, "blast_radius_percent"),
                GateStatus.valueOf(stringValue(row, "approval_status")), instantValue(row, "scheduled_at"),
                instantValue(row, "created_at"));
    }

    private ReadinessGate mapGate(Map<String, Object> row) {
        return new ReadinessGate(longValue(row, "gate_id"), stringValue(row, "service_name"),
                booleanValue(row, "runbook_ready"), booleanValue(row, "dashboard_ready"),
                booleanValue(row, "rollback_ready"), GateStatus.valueOf(stringValue(row, "status")),
                instantValue(row, "created_at"), instantValue(row, "updated_at"));
    }
}
