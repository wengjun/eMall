package com.emall.reliability;

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
        return rehearsalMapper.selectList(null);
    }

    @Override
    public SloObjective saveSlo(SloObjective slo) {
        sloMapper.insert(slo);
        return slo;
    }

    @Override
    public List<SloObjective> findSlos() {
        return sloMapper.selectList(null);
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
        return chaosMapper.selectList(null);
    }

    @Override
    public ReadinessGate saveReadinessGate(ReadinessGate gate) {
        readinessGateMapper.insert(gate);
        return gate;
    }

    @Override
    public List<ReadinessGate> findReadinessGates() {
        return readinessGateMapper.selectList(null);
    }
}
