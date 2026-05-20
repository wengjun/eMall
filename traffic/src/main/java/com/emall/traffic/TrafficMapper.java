package com.emall.traffic;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
interface TrafficMapper {
    @Insert("""
            INSERT INTO unit_cell
                (unit_id, unit_code, region_code, capacity_weight, status, created_at, updated_at)
            VALUES (#{unit.unitId}, #{unit.unitCode}, #{unit.regionCode}, #{unit.capacityWeight}, #{unit.status},
                #{unit.createdAt}, #{unit.updatedAt})
            ON DUPLICATE KEY UPDATE capacity_weight = VALUES(capacity_weight), status = VALUES(status),
                updated_at = VALUES(updated_at)
            """)
    int saveUnit(@Param("unit") UnitCell unit);

    @Select("""
            SELECT unit_id, unit_code, region_code, capacity_weight, status, created_at, updated_at
            FROM unit_cell
            WHERE unit_code = #{unitCode}
            """)
    UnitCell findUnit(@Param("unitCode") String unitCode);

    @Select("""
            SELECT unit_id, unit_code, region_code, capacity_weight, status, created_at, updated_at
            FROM unit_cell
            """)
    List<UnitCell> findUnits();

    @Insert("""
            INSERT INTO shard_route
                (route_id, domain_name, shard_no, unit_code, database_key, updated_at)
            VALUES (#{route.routeId}, #{route.domainName}, #{route.shardNo}, #{route.unitCode},
                #{route.databaseKey}, #{route.updatedAt})
            ON DUPLICATE KEY UPDATE unit_code = VALUES(unit_code), database_key = VALUES(database_key),
                updated_at = VALUES(updated_at)
            """)
    int saveRoute(@Param("route") ShardRoute route);

    @Select("""
            SELECT route_id, domain_name, shard_no, unit_code, database_key, updated_at
            FROM shard_route
            """)
    List<ShardRoute> findRoutes();

    @Insert("""
            INSERT INTO traffic_shift
                (shift_id, source_unit, target_unit, percent, status, reason, created_at, updated_at)
            VALUES (#{shift.shiftId}, #{shift.sourceUnit}, #{shift.targetUnit}, #{shift.percent}, #{shift.status},
                #{shift.reason}, #{shift.createdAt}, #{shift.updatedAt})
            ON DUPLICATE KEY UPDATE status = VALUES(status), updated_at = VALUES(updated_at)
            """)
    int saveShift(@Param("shift") TrafficShift shift);

    @Select("""
            SELECT shift_id, source_unit, target_unit, percent, status, reason, created_at, updated_at
            FROM traffic_shift
            WHERE shift_id = #{shiftId}
            """)
    TrafficShift findShift(@Param("shiftId") long shiftId);

    @Select("""
            SELECT shift_id, source_unit, target_unit, percent, status, reason, created_at, updated_at
            FROM traffic_shift
            """)
    List<TrafficShift> findShifts();

    @Insert("""
            INSERT INTO traffic_control_rule
                (rule_id, resource, type, dimension, match_value, threshold_value, unit_code, enabled, created_at,
                    updated_at)
            VALUES (#{rule.ruleId}, #{rule.resource}, #{rule.type}, #{rule.dimension}, #{rule.matchValue},
                #{rule.threshold}, #{rule.unitCode}, #{rule.enabled}, #{rule.createdAt}, #{rule.updatedAt})
            ON DUPLICATE KEY UPDATE threshold_value = VALUES(threshold_value), unit_code = VALUES(unit_code),
                enabled = VALUES(enabled), updated_at = VALUES(updated_at)
            """)
    int saveControlRule(@Param("rule") TrafficControlRule rule);

    @Select("""
            SELECT rule_id, resource, type, dimension, match_value, threshold_value AS threshold,
                unit_code, enabled, created_at, updated_at
            FROM traffic_control_rule
            WHERE rule_id = #{ruleId}
            """)
    TrafficControlRule findControlRule(@Param("ruleId") long ruleId);

    @Select("""
            SELECT rule_id, resource, type, dimension, match_value, threshold_value AS threshold,
                unit_code, enabled, created_at, updated_at
            FROM traffic_control_rule
            """)
    List<TrafficControlRule> findControlRules();
}
