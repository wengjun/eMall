package com.emall.traffic;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "memory")
class InMemoryTrafficRepository implements TrafficRepository {
    private final ConcurrentMap<String, UnitCell> units = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, ShardRoute> routes = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, TrafficShift> shifts = new ConcurrentHashMap<>();

    @Override
    public UnitCell saveUnit(UnitCell unit) {
        units.put(unit.unitCode(), unit);
        return unit;
    }

    @Override
    public Optional<UnitCell> findUnit(String unitCode) {
        return Optional.ofNullable(units.get(unitCode));
    }

    @Override
    public List<UnitCell> findUnits() {
        return List.copyOf(units.values());
    }

    @Override
    public ShardRoute saveRoute(ShardRoute route) {
        routes.put(route.routeId(), route);
        return route;
    }

    @Override
    public List<ShardRoute> findRoutes() {
        return List.copyOf(routes.values());
    }

    @Override
    public TrafficShift saveShift(TrafficShift shift) {
        shifts.put(shift.shiftId(), shift);
        return shift;
    }

    @Override
    public Optional<TrafficShift> findShift(long shiftId) {
        return Optional.ofNullable(shifts.get(shiftId));
    }

    @Override
    public List<TrafficShift> findShifts() {
        return List.copyOf(shifts.values());
    }
}
