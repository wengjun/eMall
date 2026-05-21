package com.emall.traffic;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
class MybatisPlusTrafficRepository implements TrafficRepository {
    private final TrafficMapper trafficMapper;
    private final UnitCellMapper unitMapper;
    private final ShardRouteMapper routeMapper;
    private final TrafficShiftMapper shiftMapper;
    private final TrafficControlRuleMapper controlRuleMapper;

    MybatisPlusTrafficRepository(TrafficMapper trafficMapper, UnitCellMapper unitMapper, ShardRouteMapper routeMapper,
            TrafficShiftMapper shiftMapper, TrafficControlRuleMapper controlRuleMapper) {
        this.trafficMapper = trafficMapper;
        this.unitMapper = unitMapper;
        this.routeMapper = routeMapper;
        this.shiftMapper = shiftMapper;
        this.controlRuleMapper = controlRuleMapper;
    }

    @Override
    public UnitCell saveUnit(UnitCell unit) {
        trafficMapper.saveUnit(unit);
        return unit;
    }

    @Override
    public Optional<UnitCell> findUnit(String unitCode) {
        return Optional.ofNullable(unitMapper.selectOne(new QueryWrapper<UnitCell>().eq("unit_code", unitCode)));
    }

    @Override
    public List<UnitCell> findUnits() {
        return unitMapper.selectList(null);
    }

    @Override
    public ShardRoute saveRoute(ShardRoute route) {
        trafficMapper.saveRoute(route);
        return route;
    }

    @Override
    public List<ShardRoute> findRoutes() {
        return routeMapper.selectList(null);
    }

    @Override
    public TrafficShift saveShift(TrafficShift shift) {
        trafficMapper.saveShift(shift);
        return shift;
    }

    @Override
    public Optional<TrafficShift> findShift(long shiftId) {
        return Optional.ofNullable(shiftMapper.selectById(shiftId));
    }

    @Override
    public List<TrafficShift> findShifts() {
        return shiftMapper.selectList(null);
    }

    @Override
    public TrafficControlRule saveControlRule(TrafficControlRule rule) {
        trafficMapper.saveControlRule(rule);
        return rule;
    }

    @Override
    public Optional<TrafficControlRule> findControlRule(long ruleId) {
        return Optional.ofNullable(controlRuleMapper.selectById(ruleId));
    }

    @Override
    public List<TrafficControlRule> findControlRules() {
        return controlRuleMapper.selectList(null);
    }
}
