package com.emall.traffic;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
class JdbcTrafficRepository implements TrafficRepository {
    private final JdbcTemplate jdbcTemplate;

    JdbcTrafficRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public UnitCell saveUnit(UnitCell unit) {
        jdbcTemplate.update("""
                INSERT INTO unit_cell
                    (unit_id, unit_code, region_code, capacity_weight, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE capacity_weight = VALUES(capacity_weight), status = VALUES(status),
                    updated_at = VALUES(updated_at)
                """, unit.unitId(), unit.unitCode(), unit.regionCode(), unit.capacityWeight(), unit.status().name(),
                Timestamp.from(unit.createdAt()), Timestamp.from(unit.updatedAt()));
        return unit;
    }

    @Override
    public Optional<UnitCell> findUnit(String unitCode) {
        return jdbcTemplate.query("SELECT * FROM unit_cell WHERE unit_code = ?", this::mapUnit, unitCode).stream()
                .findFirst();
    }

    @Override
    public List<UnitCell> findUnits() {
        return jdbcTemplate.query("SELECT * FROM unit_cell", this::mapUnit);
    }

    @Override
    public ShardRoute saveRoute(ShardRoute route) {
        jdbcTemplate.update("""
                INSERT INTO shard_route
                    (route_id, domain_name, shard_no, unit_code, database_key, updated_at)
                VALUES (?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE unit_code = VALUES(unit_code), database_key = VALUES(database_key),
                    updated_at = VALUES(updated_at)
                """, route.routeId(), route.domainName(), route.shardNo(), route.unitCode(), route.databaseKey(),
                Timestamp.from(route.updatedAt()));
        return route;
    }

    @Override
    public List<ShardRoute> findRoutes() {
        return jdbcTemplate.query("SELECT * FROM shard_route", this::mapRoute);
    }

    @Override
    public TrafficShift saveShift(TrafficShift shift) {
        jdbcTemplate.update("""
                INSERT INTO traffic_shift
                    (shift_id, source_unit, target_unit, percent, status, reason, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE status = VALUES(status), updated_at = VALUES(updated_at)
                """, shift.shiftId(), shift.sourceUnit(), shift.targetUnit(), shift.percent(), shift.status().name(),
                shift.reason(), Timestamp.from(shift.createdAt()), Timestamp.from(shift.updatedAt()));
        return shift;
    }

    @Override
    public Optional<TrafficShift> findShift(long shiftId) {
        return jdbcTemplate.query("SELECT * FROM traffic_shift WHERE shift_id = ?", this::mapShift, shiftId).stream()
                .findFirst();
    }

    @Override
    public List<TrafficShift> findShifts() {
        return jdbcTemplate.query("SELECT * FROM traffic_shift", this::mapShift);
    }

    private UnitCell mapUnit(ResultSet rs, int rowNum) throws SQLException {
        return new UnitCell(rs.getLong("unit_id"), rs.getString("unit_code"), rs.getString("region_code"),
                rs.getInt("capacity_weight"), UnitStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant());
    }

    private ShardRoute mapRoute(ResultSet rs, int rowNum) throws SQLException {
        return new ShardRoute(rs.getLong("route_id"), rs.getString("domain_name"), rs.getInt("shard_no"),
                rs.getString("unit_code"), rs.getString("database_key"), rs.getTimestamp("updated_at").toInstant());
    }

    private TrafficShift mapShift(ResultSet rs, int rowNum) throws SQLException {
        return new TrafficShift(rs.getLong("shift_id"), rs.getString("source_unit"), rs.getString("target_unit"),
                rs.getInt("percent"), ShiftStatus.valueOf(rs.getString("status")), rs.getString("reason"),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant());
    }
}
