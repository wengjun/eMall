package com.emall.analytics;

import com.emall.common.api.ErrorCode;
import com.emall.common.event.OutboxEvent;
import com.emall.common.exception.BusinessException;
import com.emall.common.id.SnowflakeIdGenerator;
import com.emall.common.privacy.SensitiveDataMasker;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class AnalyticsService {
    private final AnalyticsRepository repository;
    private final SnowflakeIdGenerator idGenerator;

    AnalyticsService(AnalyticsRepository repository, SnowflakeIdGenerator idGenerator) {
        this.repository = repository;
        this.idGenerator = idGenerator;
    }

    @Transactional
    MetricDefinition createMetric(String metricName, String owner, String expression) {
        Instant now = Instant.now();
        return repository.saveMetric(new MetricDefinition(idGenerator.nextId(), normalize(metricName), normalize(owner),
                expression, MetricStatus.DRAFT, now, now));
    }

    @Transactional
    MetricDefinition changeMetricStatus(long metricId, MetricStatus status) {
        MetricDefinition metric = repository.findMetric(metricId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "metric definition not found"));
        return repository.saveMetric(metric.changeStatus(status));
    }

    @Transactional
    MetricPoint recordMetricPoint(String metricName, String dimensionKey, BigDecimal metricValue, Instant eventTime) {
        requireApprovedMetric(metricName);
        return repository.saveMetricPoint(new MetricPoint(idGenerator.nextId(), normalize(metricName),
                SensitiveDataMasker.maskFreeText(normalize(dimensionKey)), metricValue, eventTime, Instant.now()));
    }

    @Transactional
    MetricPoint recordBusinessEvent(OutboxEvent event) {
        return repository.saveMetricPoint(new MetricPoint(idGenerator.nextId(), "core.event." + event.eventType(),
                SensitiveDataMasker.maskFreeText(event.aggregateType() + ":" + event.aggregateId()), BigDecimal.ONE,
                event.createdAt(), Instant.now()));
    }

    @Transactional
    DashboardDefinition createDashboard(String dashboardName, String businessDomain, String metricNames) {
        Arrays.stream(metricNames.split(",")).map(this::normalize).forEach(this::requireApprovedMetric);
        return repository.saveDashboard(new DashboardDefinition(idGenerator.nextId(), normalize(dashboardName),
                normalize(businessDomain), metricNames, Instant.now()));
    }

    @Transactional
    AnomalySignal recordAnomaly(String metricName, BigDecimal actualValue, BigDecimal expectedValue, String severity) {
        return repository.saveAnomaly(new AnomalySignal(idGenerator.nextId(), normalize(metricName), actualValue,
                expectedValue, normalize(severity), Instant.now()));
    }

    @Transactional
    ConsentRecord recordConsent(long userId, String purpose, boolean granted) {
        return repository.saveConsent(
                new ConsentRecord(idGenerator.nextId(), userId, normalize(purpose), granted, Instant.now()));
    }

    @Transactional
    PrivacyRequest openPrivacyRequest(long userId, String requestType) {
        Instant now = Instant.now();
        return repository.savePrivacyRequest(new PrivacyRequest(idGenerator.nextId(), userId, normalize(requestType),
                PrivacyRequestStatus.OPEN, now, now));
    }

    @Transactional
    PrivacyRequest changePrivacyRequestStatus(long requestId, PrivacyRequestStatus status) {
        PrivacyRequest request = repository.findPrivacyRequest(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "privacy request not found"));
        return repository.savePrivacyRequest(request.changeStatus(status));
    }

    AnalyticsSummary summary() {
        int approvedMetrics = (int) repository.findMetrics().stream()
                .filter(metric -> metric.status() == MetricStatus.APPROVED).count();
        int openPrivacyRequests = (int) repository.findPrivacyRequests().stream()
                .filter(request -> request.status() == PrivacyRequestStatus.OPEN).count();
        return new AnalyticsSummary(approvedMetrics, repository.findDashboards().size(),
                repository.findAnomalies().size(), openPrivacyRequests);
    }

    private String normalize(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "analytics value must not be blank");
        }
        return normalized;
    }

    private void requireApprovedMetric(String metricName) {
        String normalized = normalize(metricName);
        boolean approved = repository.findMetrics().stream()
                .anyMatch(metric -> metric.metricName().equals(normalized) && metric.status() == MetricStatus.APPROVED);
        if (!approved) {
            throw new BusinessException(ErrorCode.CONFLICT, "metric must be approved before reporting");
        }
    }
}
