package com.emall.platformops;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
interface PlatformOpsMapper {
    @Insert("""
            INSERT INTO backup_plan
                (plan_id, database_name, backup_type, retention_days, status, created_at, updated_at)
            VALUES (#{plan.planId}, #{plan.databaseName}, #{plan.backupType}, #{plan.retentionDays},
                #{plan.status}, #{plan.createdAt}, #{plan.updatedAt})
            ON DUPLICATE KEY UPDATE status = VALUES(status), updated_at = VALUES(updated_at)
            """)
    int saveBackupPlan(@Param("plan") BackupPlan plan);

    @Select("SELECT * FROM backup_plan WHERE plan_id = #{planId}")
    Map<String, Object> findBackupPlan(@Param("planId") long planId);

    @Select("SELECT * FROM backup_plan")
    List<Map<String, Object>> findBackupPlans();

    @Insert("""
            INSERT INTO database_operation
                (operation_id, database_name, operation_type, risk_level, status, detail, created_at, updated_at)
            VALUES (#{operation.operationId}, #{operation.databaseName}, #{operation.operationType},
                #{operation.riskLevel}, #{operation.status}, #{operation.detail}, #{operation.createdAt},
                #{operation.updatedAt})
            ON DUPLICATE KEY UPDATE status = VALUES(status), updated_at = VALUES(updated_at)
            """)
    int saveDatabaseOperation(@Param("operation") DatabaseOperation operation);

    @Select("SELECT * FROM database_operation WHERE operation_id = #{operationId}")
    Map<String, Object> findDatabaseOperation(@Param("operationId") long operationId);

    @Select("SELECT * FROM database_operation")
    List<Map<String, Object>> findDatabaseOperations();

    @Insert("""
            INSERT INTO finops_action
                (action_id, service_name, action_type, estimated_saving, status, created_at, updated_at)
            VALUES (#{action.actionId}, #{action.serviceName}, #{action.actionType}, #{action.estimatedSaving},
                #{action.status}, #{action.createdAt}, #{action.updatedAt})
            ON DUPLICATE KEY UPDATE status = VALUES(status), updated_at = VALUES(updated_at)
            """)
    int saveFinOpsAction(@Param("action") FinOpsAction action);

    @Select("SELECT * FROM finops_action WHERE action_id = #{actionId}")
    Map<String, Object> findFinOpsAction(@Param("actionId") long actionId);

    @Select("SELECT * FROM finops_action")
    List<Map<String, Object>> findFinOpsActions();

    @Insert("""
            INSERT INTO security_operation
                (operation_id, service_name, signal_type, risk_level, status, created_at, updated_at)
            VALUES (#{operation.operationId}, #{operation.serviceName}, #{operation.signalType},
                #{operation.riskLevel}, #{operation.status}, #{operation.createdAt}, #{operation.updatedAt})
            ON DUPLICATE KEY UPDATE status = VALUES(status), updated_at = VALUES(updated_at)
            """)
    int saveSecurityOperation(@Param("operation") SecurityOperation operation);

    @Select("SELECT * FROM security_operation WHERE operation_id = #{operationId}")
    Map<String, Object> findSecurityOperation(@Param("operationId") long operationId);

    @Select("SELECT * FROM security_operation")
    List<Map<String, Object>> findSecurityOperations();
}
