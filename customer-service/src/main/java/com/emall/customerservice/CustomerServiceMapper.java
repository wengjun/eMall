package com.emall.customerservice;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
interface CustomerServiceMapper {
    @Insert("""
            INSERT INTO service_ticket
                (ticket_id, user_id, order_id, category, priority, status, assignee, created_at, updated_at)
            VALUES (#{ticket.ticketId}, #{ticket.userId}, #{ticket.orderId}, #{ticket.category},
                #{ticket.priority}, #{ticket.status}, #{ticket.assignee}, #{ticket.createdAt}, #{ticket.updatedAt})
            ON DUPLICATE KEY UPDATE status = VALUES(status), assignee = VALUES(assignee),
                updated_at = VALUES(updated_at)
            """)
    int saveTicket(@Param("ticket") ServiceTicket ticket);

    @Select("""
            SELECT ticket_id, user_id, order_id, category, priority, status, assignee, created_at, updated_at
            FROM service_ticket
            WHERE ticket_id = #{ticketId}
            """)
    ServiceTicket findTicket(@Param("ticketId") long ticketId);

    @Select("""
            SELECT ticket_id, user_id, order_id, category, priority, status, assignee, created_at, updated_at
            FROM service_ticket
            """)
    List<ServiceTicket> findTickets();

    @Insert("""
            INSERT INTO arbitration_case
                (arbitration_id, ticket_id, merchant_id, reason, status, created_at, updated_at)
            VALUES (#{arbitration.arbitrationId}, #{arbitration.ticketId}, #{arbitration.merchantId},
                #{arbitration.reason}, #{arbitration.status}, #{arbitration.createdAt},
                #{arbitration.updatedAt})
            ON DUPLICATE KEY UPDATE status = VALUES(status), updated_at = VALUES(updated_at)
            """)
    int saveArbitration(@Param("arbitration") ArbitrationCase arbitration);

    @Select("""
            SELECT arbitration_id, ticket_id, merchant_id, reason, status, created_at, updated_at
            FROM arbitration_case
            WHERE arbitration_id = #{arbitrationId}
            """)
    ArbitrationCase findArbitration(@Param("arbitrationId") long arbitrationId);

    @Select("""
            SELECT arbitration_id, ticket_id, merchant_id, reason, status, created_at, updated_at
            FROM arbitration_case
            """)
    List<ArbitrationCase> findArbitrations();

    @Insert("""
            INSERT INTO compensation_record
                (compensation_id, ticket_id, user_id, amount, reason, created_at)
            VALUES (#{compensation.compensationId}, #{compensation.ticketId}, #{compensation.userId},
                #{compensation.amount}, #{compensation.reason}, #{compensation.createdAt})
            """)
    int saveCompensation(@Param("compensation") CompensationRecord compensation);

    @Select("""
            SELECT compensation_id, ticket_id, user_id, amount, reason, created_at
            FROM compensation_record
            """)
    List<CompensationRecord> findCompensations();

    @Insert("""
            INSERT INTO knowledge_article
                (article_id, category, title, content, published, created_at, updated_at)
            VALUES (#{article.articleId}, #{article.category}, #{article.title}, #{article.content},
                #{article.published}, #{article.createdAt}, #{article.updatedAt})
            ON DUPLICATE KEY UPDATE content = VALUES(content), published = VALUES(published),
                updated_at = VALUES(updated_at)
            """)
    int saveArticle(@Param("article") KnowledgeArticle article);

    @Insert("""
            INSERT INTO service_quality_review (review_id, ticket_id, score, comment, created_at)
            VALUES (#{review.reviewId}, #{review.ticketId}, #{review.score}, #{review.comment},
                #{review.createdAt})
            """)
    int saveReview(@Param("review") ServiceQualityReview review);
}
