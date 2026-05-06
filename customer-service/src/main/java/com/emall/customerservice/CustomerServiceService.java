package com.emall.customerservice;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import com.emall.common.id.SnowflakeIdGenerator;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CustomerServiceService {
    private final CustomerServiceRepository repository;
    private final SnowflakeIdGenerator idGenerator;

    CustomerServiceService(CustomerServiceRepository repository, SnowflakeIdGenerator idGenerator) {
        this.repository = repository;
        this.idGenerator = idGenerator;
    }

    @Transactional
    ServiceTicket createTicket(long userId, long orderId, String category, String priority) {
        Instant now = Instant.now();
        return repository.saveTicket(new ServiceTicket(idGenerator.nextId(), userId, orderId, normalize(category),
                normalize(priority), TicketStatus.OPEN, "", now, now));
    }

    @Transactional
    ServiceTicket routeTicket(long ticketId, String assignee) {
        ServiceTicket ticket = requireTicket(ticketId);
        return repository.saveTicket(ticket.route(normalize(assignee)));
    }

    @Transactional
    ServiceTicket resolveTicket(long ticketId) {
        return repository.saveTicket(requireTicket(ticketId).resolve());
    }

    @Transactional
    ArbitrationCase openArbitration(long ticketId, long merchantId, String reason) {
        requireTicket(ticketId);
        Instant now = Instant.now();
        return repository.saveArbitration(new ArbitrationCase(idGenerator.nextId(), ticketId, merchantId, reason,
                ArbitrationStatus.OPEN, now, now));
    }

    @Transactional
    ArbitrationCase closeArbitration(long arbitrationId, ArbitrationStatus status) {
        ArbitrationCase arbitration = repository.findArbitration(arbitrationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "arbitration case not found"));
        return repository.saveArbitration(arbitration.close(status));
    }

    @Transactional
    CompensationRecord grantCompensation(long ticketId, long userId, BigDecimal amount, String reason) {
        requireTicket(ticketId);
        if (amount.signum() <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "compensation amount must be positive");
        }
        return repository.saveCompensation(new CompensationRecord(idGenerator.nextId(), ticketId, userId, amount,
                reason, Instant.now()));
    }

    @Transactional
    KnowledgeArticle publishArticle(String category, String title, String content) {
        Instant now = Instant.now();
        return repository.saveArticle(new KnowledgeArticle(idGenerator.nextId(), normalize(category), title,
                content, true, now, now));
    }

    @Transactional
    ServiceQualityReview reviewService(long ticketId, int score, String comment) {
        requireTicket(ticketId);
        if (score < 1 || score > 5) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "service score must be 1-5");
        }
        return repository.saveReview(new ServiceQualityReview(idGenerator.nextId(), ticketId, score, comment,
                Instant.now()));
    }

    CustomerServiceSummary summary() {
        int open = (int) repository.findTickets().stream().filter(ticket -> ticket.status() == TicketStatus.OPEN)
                .count();
        int routed = (int) repository.findTickets().stream().filter(ticket -> ticket.status() == TicketStatus.ROUTED)
                .count();
        int arbitration = (int) repository.findArbitrations().stream()
                .filter(item -> item.status() == ArbitrationStatus.OPEN)
                .count();
        return new CustomerServiceSummary(open, routed, arbitration, repository.findCompensations().size());
    }

    private ServiceTicket requireTicket(long ticketId) {
        return repository.findTicket(ticketId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "service ticket not found"));
    }

    private String normalize(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "customer service value must not be blank");
        }
        return normalized;
    }
}
