package com.emall.reliability;

import com.emall.common.persistence.BoundedQuery;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
class MybatisPlusReliabilityRepository implements ReliabilityRepository {
    private final ReliabilityMapper reliabilityMapper;
    private final CapacityRehearsalMapper rehearsalMapper;
    private final SloObjectiveMapper sloMapper;
    private final ChaosScheduleMapper chaosMapper;
    private final ReadinessGateMapper readinessGateMapper;

    MybatisPlusReliabilityRepository(ReliabilityMapper reliabilityMapper, CapacityRehearsalMapper rehearsalMapper,
            SloObjectiveMapper sloMapper, ChaosScheduleMapper chaosMapper, ReadinessGateMapper readinessGateMapper) {
        this.reliabilityMapper = reliabilityMapper;
        this.rehearsalMapper = rehearsalMapper;
        this.sloMapper = sloMapper;
        this.chaosMapper = chaosMapper;
        this.readinessGateMapper = readinessGateMapper;
    }

    @Override
    public CapacityRehearsal saveRehearsal(CapacityRehearsal rehearsal) {
        reliabilityMapper.saveRehearsal(rehearsal);
        return rehearsal;
    }

    @Override
    public Optional<CapacityRehearsal> findRehearsal(long rehearsalId) {
        return Optional.ofNullable(rehearsalMapper.selectById(rehearsalId));
    }

    @Override
    public List<CapacityRehearsal> findRehearsals() {
        return BoundedQuery.firstPage(rehearsalMapper);
    }

    @Override
    public SloObjective saveSlo(SloObjective slo) {
        sloMapper.insert(slo);
        return slo;
    }

    @Override
    public List<SloObjective> findSlos() {
        return BoundedQuery.firstPage(sloMapper);
    }

    @Override
    public ChaosSchedule saveChaos(ChaosSchedule chaos) {
        reliabilityMapper.saveChaos(chaos);
        return chaos;
    }

    @Override
    public Optional<ChaosSchedule> findChaos(long chaosId) {
        return Optional.ofNullable(chaosMapper.selectById(chaosId));
    }

    @Override
    public List<ChaosSchedule> findChaosSchedules() {
        return BoundedQuery.firstPage(chaosMapper);
    }

    @Override
    public ReadinessGate saveReadinessGate(ReadinessGate gate) {
        readinessGateMapper.insert(gate);
        return gate;
    }

    @Override
    public List<ReadinessGate> findReadinessGates() {
        return BoundedQuery.firstPage(readinessGateMapper);
    }
}
