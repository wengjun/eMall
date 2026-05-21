package com.emall.reliability;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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

    @Insert("""
            INSERT INTO chaos_schedule
                (chaos_id, service_name, drill_type, blast_radius_percent, approval_status, scheduled_at,
                created_at)
            VALUES (#{chaos.chaosId}, #{chaos.serviceName}, #{chaos.drillType},
                #{chaos.blastRadiusPercent}, #{chaos.approvalStatus}, #{chaos.scheduledAt}, #{chaos.createdAt})
            ON DUPLICATE KEY UPDATE approval_status = VALUES(approval_status)
            """)
    int saveChaos(@Param("chaos") ChaosSchedule chaos);
}
