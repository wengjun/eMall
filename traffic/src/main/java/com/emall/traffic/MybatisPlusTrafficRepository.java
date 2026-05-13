package com.emall.traffic;

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
        return Optional.ofNullable(trafficMapper.findUnit(unitCode)).map(this::mapUnit);
    }

    @Override
    public List<UnitCell> findUnits() {
        return trafficMapper.findUnits().stream().map(this::mapUnit).toList();
    }

    @Override
    public ShardRoute saveRoute(ShardRoute route) {
        trafficMapper.saveRoute(route);
        return route;
    }

    @Override
    public List<ShardRoute> findRoutes() {
        return trafficMapper.findRoutes().stream().map(this::mapRoute).toList();
    }

    @Override
    public TrafficShift saveShift(TrafficShift shift) {
        trafficMapper.saveShift(shift);
        return shift;
    }

    @Override
    public Optional<TrafficShift> findShift(long shiftId) {
        return Optional.ofNullable(trafficMapper.findShift(shiftId)).map(this::mapShift);
    }

    @Override
    public List<TrafficShift> findShifts() {
        return trafficMapper.findShifts().stream().map(this::mapShift).toList();
    }

    private UnitCell mapUnit(Map<String, Object> row) {
        return new UnitCell(longValue(row, "unit_id"), stringValue(row, "unit_code"),
                stringValue(row, "region_code"), intValue(row, "capacity_weight"),
                UnitStatus.valueOf(stringValue(row, "status")), instantValue(row, "created_at"),
                instantValue(row, "updated_at"));
    }

    private ShardRoute mapRoute(Map<String, Object> row) {
        return new ShardRoute(longValue(row, "route_id"), stringValue(row, "domain_name"),
                intValue(row, "shard_no"), stringValue(row, "unit_code"), stringValue(row, "database_key"),
                instantValue(row, "updated_at"));
    }

    private TrafficShift mapShift(Map<String, Object> row) {
        return new TrafficShift(longValue(row, "shift_id"), stringValue(row, "source_unit"),
                stringValue(row, "target_unit"), intValue(row, "percent"),
                ShiftStatus.valueOf(stringValue(row, "status")), stringValue(row, "reason"),
                instantValue(row, "created_at"), instantValue(row, "updated_at"));
    }
}
