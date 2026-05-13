package com.emall.reliability;

import java.util.List;
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
        return Optional.ofNullable(reliabilityMapper.findRehearsal(rehearsalId));
    }

    @Override
    public List<CapacityRehearsal> findRehearsals() {
        return reliabilityMapper.findRehearsals();
    }

    @Override
    public SloObjective saveSlo(SloObjective slo) {
        reliabilityMapper.saveSlo(slo);
        return slo;
    }

    @Override
    public List<SloObjective> findSlos() {
        return reliabilityMapper.findSlos();
    }

    @Override
    public ChaosSchedule saveChaos(ChaosSchedule chaos) {
        reliabilityMapper.saveChaos(chaos);
        return chaos;
    }

    @Override
    public Optional<ChaosSchedule> findChaos(long chaosId) {
        return Optional.ofNullable(reliabilityMapper.findChaos(chaosId));
    }

    @Override
    public List<ChaosSchedule> findChaosSchedules() {
        return reliabilityMapper.findChaosSchedules();
    }

    @Override
    public ReadinessGate saveReadinessGate(ReadinessGate gate) {
        reliabilityMapper.saveReadinessGate(gate);
        return gate;
    }

    @Override
    public List<ReadinessGate> findReadinessGates() {
        return reliabilityMapper.findReadinessGates();
    }
}
