package com.emall.reliability;

import java.util.List;
import java.util.Optional;

interface ReliabilityRepository {
    CapacityRehearsal saveRehearsal(CapacityRehearsal rehearsal);

    Optional<CapacityRehearsal> findRehearsal(long rehearsalId);

    List<CapacityRehearsal> findRehearsals();

    SloObjective saveSlo(SloObjective slo);

    List<SloObjective> findSlos();

    ChaosSchedule saveChaos(ChaosSchedule chaos);

    Optional<ChaosSchedule> findChaos(long chaosId);

    List<ChaosSchedule> findChaosSchedules();

    ReadinessGate saveReadinessGate(ReadinessGate gate);

    List<ReadinessGate> findReadinessGates();
}
