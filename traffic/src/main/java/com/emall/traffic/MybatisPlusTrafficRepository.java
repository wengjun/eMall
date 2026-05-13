package com.emall.traffic;

import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
class MybatisPlusTrafficRepository implements TrafficRepository {
    private final TrafficMapper trafficMapper;

    MybatisPlusTrafficRepository(TrafficMapper trafficMapper) {
        this.trafficMapper = trafficMapper;
    }

    @Override
    public UnitCell saveUnit(UnitCell unit) {
        trafficMapper.saveUnit(unit);
        return unit;
    }

    @Override
    public Optional<UnitCell> findUnit(String unitCode) {
        return Optional.ofNullable(trafficMapper.findUnit(unitCode));
    }

    @Override
    public List<UnitCell> findUnits() {
        return trafficMapper.findUnits();
    }

    @Override
    public ShardRoute saveRoute(ShardRoute route) {
        trafficMapper.saveRoute(route);
        return route;
    }

    @Override
    public List<ShardRoute> findRoutes() {
        return trafficMapper.findRoutes();
    }

    @Override
    public TrafficShift saveShift(TrafficShift shift) {
        trafficMapper.saveShift(shift);
        return shift;
    }

    @Override
    public Optional<TrafficShift> findShift(long shiftId) {
        return Optional.ofNullable(trafficMapper.findShift(shiftId));
    }

    @Override
    public List<TrafficShift> findShifts() {
        return trafficMapper.findShifts();
    }
}
