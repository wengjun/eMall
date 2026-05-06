package com.emall.reliability;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "memory")
class InMemoryReliabilityRepository implements ReliabilityRepository {
    private final ConcurrentMap<Long, CapacityRehearsal> rehearsals = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, SloObjective> slos = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, ChaosSchedule> chaosSchedules = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, ReadinessGate> gates = new ConcurrentHashMap<>();

    @Override
    public CapacityRehearsal saveRehearsal(CapacityRehearsal rehearsal) {
        rehearsals.put(rehearsal.rehearsalId(), rehearsal);
        return rehearsal;
    }

    @Override
    public Optional<CapacityRehearsal> findRehearsal(long rehearsalId) {
        return Optional.ofNullable(rehearsals.get(rehearsalId));
    }

    @Override
    public List<CapacityRehearsal> findRehearsals() {
        return List.copyOf(rehearsals.values());
    }

    @Override
    public SloObjective saveSlo(SloObjective slo) {
        slos.put(slo.sloId(), slo);
        return slo;
    }

    @Override
    public List<SloObjective> findSlos() {
        return List.copyOf(slos.values());
    }

    @Override
    public ChaosSchedule saveChaos(ChaosSchedule chaos) {
        chaosSchedules.put(chaos.chaosId(), chaos);
        return chaos;
    }

    @Override
    public Optional<ChaosSchedule> findChaos(long chaosId) {
        return Optional.ofNullable(chaosSchedules.get(chaosId));
    }

    @Override
    public List<ChaosSchedule> findChaosSchedules() {
        return List.copyOf(chaosSchedules.values());
    }

    @Override
    public ReadinessGate saveReadinessGate(ReadinessGate gate) {
        gates.put(gate.gateId(), gate);
        return gate;
    }

    @Override
    public List<ReadinessGate> findReadinessGates() {
        return List.copyOf(gates.values());
    }
}
