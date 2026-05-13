package com.emall.reliability;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
interface ReliabilityMapper {
    @Insert("""
            INSERT INTO capacity_rehearsal
                (rehearsal_id, service_name, target_qps, peak_concurrency, status, created_at, updated_at)
            VALUES (#{rehearsal.rehearsalId}, #{rehearsal.serviceName}, #{rehearsal.targetQps},
                #{rehearsal.peakConcurrency}, #{rehearsal.status}, #{rehearsal.createdAt},
                #{rehearsal.updatedAt})
            ON DUPLICATE KEY UPDATE status = VALUES(status), updated_at = VALUES(updated_at)
            """)
    int saveRehearsal(@Param("rehearsal") CapacityRehearsal rehearsal);

    @Select("SELECT * FROM capacity_rehearsal WHERE rehearsal_id = #{rehearsalId}")
    Map<String, Object> findRehearsal(@Param("rehearsalId") long rehearsalId);

    @Select("SELECT * FROM capacity_rehearsal")
    List<Map<String, Object>> findRehearsals();

    @Insert("""
            INSERT INTO slo_objective
                (slo_id, service_name, availability_target, latency_p95_ms, error_budget_percent, created_at)
            VALUES (#{slo.sloId}, #{slo.serviceName}, #{slo.availabilityTarget}, #{slo.latencyP95Ms},
                #{slo.errorBudgetPercent}, #{slo.createdAt})
            """)
    int saveSlo(@Param("slo") SloObjective slo);

    @Select("SELECT * FROM slo_objective")
    List<Map<String, Object>> findSlos();

    @Insert("""
            INSERT INTO chaos_schedule
                (chaos_id, service_name, drill_type, blast_radius_percent, approval_status, scheduled_at,
                created_at)
            VALUES (#{chaos.chaosId}, #{chaos.serviceName}, #{chaos.drillType},
                #{chaos.blastRadiusPercent}, #{chaos.approvalStatus}, #{chaos.scheduledAt}, #{chaos.createdAt})
            ON DUPLICATE KEY UPDATE approval_status = VALUES(approval_status)
            """)
    int saveChaos(@Param("chaos") ChaosSchedule chaos);

    @Select("SELECT * FROM chaos_schedule WHERE chaos_id = #{chaosId}")
    Map<String, Object> findChaos(@Param("chaosId") long chaosId);

    @Select("SELECT * FROM chaos_schedule")
    List<Map<String, Object>> findChaosSchedules();

    @Insert("""
            INSERT INTO readiness_gate
                (gate_id, service_name, runbook_ready, dashboard_ready, rollback_ready, status, created_at,
                updated_at)
            VALUES (#{gate.gateId}, #{gate.serviceName}, #{gate.runbookReady}, #{gate.dashboardReady},
                #{gate.rollbackReady}, #{gate.status}, #{gate.createdAt}, #{gate.updatedAt})
            """)
    int saveReadinessGate(@Param("gate") ReadinessGate gate);

    @Select("SELECT * FROM readiness_gate")
    List<Map<String, Object>> findReadinessGates();
}
