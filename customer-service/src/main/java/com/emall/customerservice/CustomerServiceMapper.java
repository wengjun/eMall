package com.emall.customerservice;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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

    @Insert("""
            INSERT INTO arbitration_case
                (arbitration_id, ticket_id, merchant_id, reason, status, created_at, updated_at)
            VALUES (#{arbitration.arbitrationId}, #{arbitration.ticketId}, #{arbitration.merchantId},
                #{arbitration.reason}, #{arbitration.status}, #{arbitration.createdAt},
                #{arbitration.updatedAt})
            ON DUPLICATE KEY UPDATE status = VALUES(status), updated_at = VALUES(updated_at)
            """)
    int saveArbitration(@Param("arbitration") ArbitrationCase arbitration);

    @Insert("""
            INSERT INTO knowledge_article
                (article_id, category, title, content, published, created_at, updated_at)
            VALUES (#{article.articleId}, #{article.category}, #{article.title}, #{article.content},
                #{article.published}, #{article.createdAt}, #{article.updatedAt})
            ON DUPLICATE KEY UPDATE content = VALUES(content), published = VALUES(published),
                updated_at = VALUES(updated_at)
            """)
    int saveArticle(@Param("article") KnowledgeArticle article);
}
