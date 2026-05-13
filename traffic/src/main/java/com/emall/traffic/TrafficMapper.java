package com.emall.traffic;

import java.util.List;
import java.util.Map;
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

    @Select("SELECT * FROM unit_cell WHERE unit_code = #{unitCode}")
    Map<String, Object> findUnit(@Param("unitCode") String unitCode);

    @Select("SELECT * FROM unit_cell")
    List<Map<String, Object>> findUnits();

    @Insert("""
            INSERT INTO shard_route
                (route_id, domain_name, shard_no, unit_code, database_key, updated_at)
            VALUES (#{route.routeId}, #{route.domainName}, #{route.shardNo}, #{route.unitCode},
                #{route.databaseKey}, #{route.updatedAt})
            ON DUPLICATE KEY UPDATE unit_code = VALUES(unit_code), database_key = VALUES(database_key),
                updated_at = VALUES(updated_at)
            """)
    int saveRoute(@Param("route") ShardRoute route);

    @Select("SELECT * FROM shard_route")
    List<Map<String, Object>> findRoutes();

    @Insert("""
            INSERT INTO traffic_shift
                (shift_id, source_unit, target_unit, percent, status, reason, created_at, updated_at)
            VALUES (#{shift.shiftId}, #{shift.sourceUnit}, #{shift.targetUnit}, #{shift.percent}, #{shift.status},
                #{shift.reason}, #{shift.createdAt}, #{shift.updatedAt})
            ON DUPLICATE KEY UPDATE status = VALUES(status), updated_at = VALUES(updated_at)
            """)
    int saveShift(@Param("shift") TrafficShift shift);

    @Select("SELECT * FROM traffic_shift WHERE shift_id = #{shiftId}")
    Map<String, Object> findShift(@Param("shiftId") long shiftId);

    @Select("SELECT * FROM traffic_shift")
    List<Map<String, Object>> findShifts();
}
