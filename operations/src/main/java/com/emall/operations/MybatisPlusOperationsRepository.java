package com.emall.operations;

import java.util.List;
import java.util.Optional;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
class MybatisPlusOperationsRepository implements OperationsRepository {
    private final OperationsMapper operationsMapper;
    private final ApprovalRequestMapper approvalMapper;
    private final OperationTaskMapper taskMapper;
    private final ComplianceEvidenceMapper evidenceMapper;
    private final SecurityIncidentMapper incidentMapper;

    MybatisPlusOperationsRepository(OperationsMapper operationsMapper, ApprovalRequestMapper approvalMapper,
            OperationTaskMapper taskMapper, ComplianceEvidenceMapper evidenceMapper,
            SecurityIncidentMapper incidentMapper) {
        this.operationsMapper = operationsMapper;
        this.approvalMapper = approvalMapper;
        this.taskMapper = taskMapper;
        this.evidenceMapper = evidenceMapper;
        this.incidentMapper = incidentMapper;
    }

    @Override
    public ApprovalRequest saveApproval(ApprovalRequest approval) {
        operationsMapper.saveApproval(approval);
        return approval;
    }

    @Override
    public Optional<ApprovalRequest> findApproval(long approvalId) {
        return Optional.ofNullable(approvalMapper.selectById(approvalId));
    }

    @Override
    public List<ApprovalRequest> findApprovals(ApprovalStatus status) {
        return approvalMapper.selectList(
                new QueryWrapper<ApprovalRequest>().eq("status", status).orderByDesc("updated_at"));
    }

    @Override
    public OperationTask saveTask(OperationTask task) {
        operationsMapper.saveTask(task);
        return task;
    }

    @Override
    public Optional<OperationTask> findTask(long taskId) {
        return Optional.ofNullable(taskMapper.selectById(taskId));
    }

    @Override
    public List<OperationTask> findTasks(TaskStatus status) {
        return taskMapper.selectList(new QueryWrapper<OperationTask>()
                .eq("status", status)
                .orderByAsc("priority")
                .orderByDesc("updated_at"));
    }

    @Override
    public ComplianceEvidence saveEvidence(ComplianceEvidence evidence) {
        evidenceMapper.insert(evidence);
        return evidence;
    }

    @Override
    public List<ComplianceEvidence> findEvidence(String resourceType, String resourceId) {
        return evidenceMapper.selectList(new QueryWrapper<ComplianceEvidence>()
                .eq("resource_type", resourceType)
                .eq("resource_id", resourceId)
                .orderByDesc("created_at"));
    }

    @Override
    public SecurityIncident saveIncident(SecurityIncident incident) {
        operationsMapper.saveIncident(incident);
        return incident;
    }

    @Override
    public Optional<SecurityIncident> findIncident(long incidentId) {
        return Optional.ofNullable(incidentMapper.selectById(incidentId));
    }
}
