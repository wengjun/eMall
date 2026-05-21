package com.emall.customerservice;

import java.math.BigDecimal;
import java.time.Instant;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

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

@TableName("service_ticket")
record ServiceTicket(@TableId(value = "ticket_id", type = IdType.INPUT) long ticketId, long userId, long orderId,
        String category, String priority, TicketStatus status, String assignee, Instant createdAt, Instant updatedAt) {
    ServiceTicket route(String nextAssignee) {
        return new ServiceTicket(ticketId, userId, orderId, category, priority, TicketStatus.ROUTED, nextAssignee,
                createdAt, Instant.now());
    }

    ServiceTicket resolve() {
        return new ServiceTicket(ticketId, userId, orderId, category, priority, TicketStatus.RESOLVED, assignee,
                createdAt, Instant.now());
    }
}

@TableName("arbitration_case")
record ArbitrationCase(@TableId(value = "arbitration_id", type = IdType.INPUT) long arbitrationId, long ticketId,
        long merchantId, String reason, ArbitrationStatus status, Instant createdAt, Instant updatedAt) {
    ArbitrationCase close(ArbitrationStatus nextStatus) {
        return new ArbitrationCase(arbitrationId, ticketId, merchantId, reason, nextStatus, createdAt, Instant.now());
    }
}

@TableName("compensation_record")
record CompensationRecord(@TableId(value = "compensation_id", type = IdType.INPUT) long compensationId, long ticketId,
        long userId, BigDecimal amount, String reason, Instant createdAt) {
}

@TableName("knowledge_article")
record KnowledgeArticle(@TableId(value = "article_id", type = IdType.INPUT) long articleId, String category,
        String title, String content, boolean published,
        Instant createdAt, Instant updatedAt) {
}

@TableName("service_quality_review")
record ServiceQualityReview(@TableId(value = "review_id", type = IdType.INPUT) long reviewId, long ticketId, int score,
        String comment, Instant createdAt) {
}

record CustomerServiceSummary(int openTickets, int routedTickets, int openArbitrations, int compensationCount) {
}
