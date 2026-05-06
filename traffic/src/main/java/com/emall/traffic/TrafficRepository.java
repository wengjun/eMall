package com.emall.traffic;

import java.util.List;
import java.util.Optional;

interface TrafficRepository {
    UnitCell saveUnit(UnitCell unit);

    Optional<UnitCell> findUnit(String unitCode);

    List<UnitCell> findUnits();

    ShardRoute saveRoute(ShardRoute route);

    List<ShardRoute> findRoutes();

    TrafficShift saveShift(TrafficShift shift);

    Optional<TrafficShift> findShift(long shiftId);

    List<TrafficShift> findShifts();
}
