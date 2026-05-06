package com.emall.customerservice;

import java.math.BigDecimal;
import java.time.Instant;

enum TicketStatus {
    OPEN,
    ROUTED,
    RESOLVED,
    ESCALATED
}

enum ArbitrationStatus {
    OPEN,
    CUSTOMER_WON,
    MERCHANT_WON,
    PLATFORM_MEDIATED
}

record ServiceTicket(long ticketId, long userId, long orderId, String category, String priority,
                     TicketStatus status, String assignee, Instant createdAt, Instant updatedAt) {
    ServiceTicket route(String nextAssignee) {
        return new ServiceTicket(ticketId, userId, orderId, category, priority, TicketStatus.ROUTED, nextAssignee,
                createdAt, Instant.now());
    }

    ServiceTicket resolve() {
        return new ServiceTicket(ticketId, userId, orderId, category, priority, TicketStatus.RESOLVED, assignee,
                createdAt, Instant.now());
    }
}

record ArbitrationCase(long arbitrationId, long ticketId, long merchantId, String reason, ArbitrationStatus status,
                       Instant createdAt, Instant updatedAt) {
    ArbitrationCase close(ArbitrationStatus nextStatus) {
        return new ArbitrationCase(arbitrationId, ticketId, merchantId, reason, nextStatus, createdAt,
                Instant.now());
    }
}

record CompensationRecord(long compensationId, long ticketId, long userId, BigDecimal amount, String reason,
                          Instant createdAt) {
}

record KnowledgeArticle(long articleId, String category, String title, String content, boolean published,
                        Instant createdAt, Instant updatedAt) {
}

record ServiceQualityReview(long reviewId, long ticketId, int score, String comment, Instant createdAt) {
}

record CustomerServiceSummary(int openTickets, int routedTickets, int openArbitrations, int compensationCount) {
}
