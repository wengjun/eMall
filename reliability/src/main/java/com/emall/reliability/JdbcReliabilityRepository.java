package com.emall.reliability;

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
class JdbcReliabilityRepository implements ReliabilityRepository {
    private final JdbcTemplate jdbcTemplate;

    JdbcReliabilityRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public CapacityRehearsal saveRehearsal(CapacityRehearsal rehearsal) {
        jdbcTemplate.update("""
                INSERT INTO capacity_rehearsal
                    (rehearsal_id, service_name, target_qps, peak_concurrency, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE status = VALUES(status), updated_at = VALUES(updated_at)
                """, rehearsal.rehearsalId(), rehearsal.serviceName(), rehearsal.targetQps(),
                rehearsal.peakConcurrency(), rehearsal.status().name(), Timestamp.from(rehearsal.createdAt()),
                Timestamp.from(rehearsal.updatedAt()));
        return rehearsal;
    }

    @Override
    public Optional<CapacityRehearsal> findRehearsal(long rehearsalId) {
        return jdbcTemplate.query("SELECT * FROM capacity_rehearsal WHERE rehearsal_id = ?",
                this::mapRehearsal, rehearsalId).stream().findFirst();
    }

    @Override
    public List<CapacityRehearsal> findRehearsals() {
        return jdbcTemplate.query("SELECT * FROM capacity_rehearsal", this::mapRehearsal);
    }

    @Override
    public SloObjective saveSlo(SloObjective slo) {
        jdbcTemplate.update("""
                INSERT INTO slo_objective
                    (slo_id, service_name, availability_target, latency_p95_ms, error_budget_percent, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, slo.sloId(), slo.serviceName(), slo.availabilityTarget(), slo.latencyP95Ms(),
                slo.errorBudgetPercent(), Timestamp.from(slo.createdAt()));
        return slo;
    }

    @Override
    public List<SloObjective> findSlos() {
        return jdbcTemplate.query("SELECT * FROM slo_objective", this::mapSlo);
    }

    @Override
    public ChaosSchedule saveChaos(ChaosSchedule chaos) {
        jdbcTemplate.update("""
                INSERT INTO chaos_schedule
                    (chaos_id, service_name, drill_type, blast_radius_percent, approval_status, scheduled_at,
                    created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE approval_status = VALUES(approval_status)
                """, chaos.chaosId(), chaos.serviceName(), chaos.drillType(), chaos.blastRadiusPercent(),
                chaos.approvalStatus().name(), Timestamp.from(chaos.scheduledAt()), Timestamp.from(chaos.createdAt()));
        return chaos;
    }

    @Override
    public Optional<ChaosSchedule> findChaos(long chaosId) {
        return jdbcTemplate.query("SELECT * FROM chaos_schedule WHERE chaos_id = ?", this::mapChaos, chaosId)
                .stream().findFirst();
    }

    @Override
    public List<ChaosSchedule> findChaosSchedules() {
        return jdbcTemplate.query("SELECT * FROM chaos_schedule", this::mapChaos);
    }

    @Override
    public ReadinessGate saveReadinessGate(ReadinessGate gate) {
        jdbcTemplate.update("""
                INSERT INTO readiness_gate
                    (gate_id, service_name, runbook_ready, dashboard_ready, rollback_ready, status, created_at,
                    updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, gate.gateId(), gate.serviceName(), gate.runbookReady(), gate.dashboardReady(),
                gate.rollbackReady(), gate.status().name(), Timestamp.from(gate.createdAt()),
                Timestamp.from(gate.updatedAt()));
        return gate;
    }

    @Override
    public List<ReadinessGate> findReadinessGates() {
        return jdbcTemplate.query("SELECT * FROM readiness_gate", this::mapGate);
    }

    private CapacityRehearsal mapRehearsal(ResultSet rs, int rowNum) throws SQLException {
        return new CapacityRehearsal(rs.getLong("rehearsal_id"), rs.getString("service_name"),
                rs.getInt("target_qps"), rs.getInt("peak_concurrency"), GateStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant());
    }

    private SloObjective mapSlo(ResultSet rs, int rowNum) throws SQLException {
        return new SloObjective(rs.getLong("slo_id"), rs.getString("service_name"),
                rs.getBigDecimal("availability_target"), rs.getInt("latency_p95_ms"),
                rs.getBigDecimal("error_budget_percent"), rs.getTimestamp("created_at").toInstant());
    }

    private ChaosSchedule mapChaos(ResultSet rs, int rowNum) throws SQLException {
        return new ChaosSchedule(rs.getLong("chaos_id"), rs.getString("service_name"),
                rs.getString("drill_type"), rs.getInt("blast_radius_percent"),
                GateStatus.valueOf(rs.getString("approval_status")), rs.getTimestamp("scheduled_at").toInstant(),
                rs.getTimestamp("created_at").toInstant());
    }

    private ReadinessGate mapGate(ResultSet rs, int rowNum) throws SQLException {
        return new ReadinessGate(rs.getLong("gate_id"), rs.getString("service_name"),
                rs.getBoolean("runbook_ready"), rs.getBoolean("dashboard_ready"), rs.getBoolean("rollback_ready"),
                GateStatus.valueOf(rs.getString("status")), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }
}
