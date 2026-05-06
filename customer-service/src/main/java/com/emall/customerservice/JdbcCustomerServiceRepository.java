package com.emall.customerservice;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
class JdbcCustomerServiceRepository implements CustomerServiceRepository {
    private final JdbcTemplate jdbcTemplate;

    JdbcCustomerServiceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public ServiceTicket saveTicket(ServiceTicket ticket) {
        jdbcTemplate.update("""
                INSERT INTO service_ticket
                    (ticket_id, user_id, order_id, category, priority, status, assignee, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE status = VALUES(status), assignee = VALUES(assignee),
                    updated_at = VALUES(updated_at)
                """, ticket.ticketId(), ticket.userId(), ticket.orderId(), ticket.category(), ticket.priority(),
                ticket.status().name(), ticket.assignee(), Timestamp.from(ticket.createdAt()),
                Timestamp.from(ticket.updatedAt()));
        return ticket;
    }

    @Override
    public Optional<ServiceTicket> findTicket(long ticketId) {
        return jdbcTemplate.query("SELECT * FROM service_ticket WHERE ticket_id = ?", this::mapTicket, ticketId)
                .stream().findFirst();
    }

    @Override
    public List<ServiceTicket> findTickets() {
        return jdbcTemplate.query("SELECT * FROM service_ticket", this::mapTicket);
    }

    @Override
    public ArbitrationCase saveArbitration(ArbitrationCase arbitration) {
        jdbcTemplate.update("""
                INSERT INTO arbitration_case
                    (arbitration_id, ticket_id, merchant_id, reason, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE status = VALUES(status), updated_at = VALUES(updated_at)
                """, arbitration.arbitrationId(), arbitration.ticketId(), arbitration.merchantId(),
                arbitration.reason(), arbitration.status().name(), Timestamp.from(arbitration.createdAt()),
                Timestamp.from(arbitration.updatedAt()));
        return arbitration;
    }

    @Override
    public Optional<ArbitrationCase> findArbitration(long arbitrationId) {
        return jdbcTemplate.query("SELECT * FROM arbitration_case WHERE arbitration_id = ?", this::mapArbitration,
                arbitrationId).stream().findFirst();
    }

    @Override
    public List<ArbitrationCase> findArbitrations() {
        return jdbcTemplate.query("SELECT * FROM arbitration_case", this::mapArbitration);
    }

    @Override
    public CompensationRecord saveCompensation(CompensationRecord compensation) {
        jdbcTemplate.update("""
                INSERT INTO compensation_record
                    (compensation_id, ticket_id, user_id, amount, reason, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, compensation.compensationId(), compensation.ticketId(), compensation.userId(),
                compensation.amount(), compensation.reason(), Timestamp.from(compensation.createdAt()));
        return compensation;
    }

    @Override
    public List<CompensationRecord> findCompensations() {
        return jdbcTemplate.query("SELECT * FROM compensation_record", this::mapCompensation);
    }

    @Override
    public KnowledgeArticle saveArticle(KnowledgeArticle article) {
        jdbcTemplate.update("""
                INSERT INTO knowledge_article
                    (article_id, category, title, content, published, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE content = VALUES(content), published = VALUES(published),
                    updated_at = VALUES(updated_at)
                """, article.articleId(), article.category(), article.title(), article.content(),
                article.published(), Timestamp.from(article.createdAt()), Timestamp.from(article.updatedAt()));
        return article;
    }

    @Override
    public ServiceQualityReview saveReview(ServiceQualityReview review) {
        jdbcTemplate.update("""
                INSERT INTO service_quality_review (review_id, ticket_id, score, comment, created_at)
                VALUES (?, ?, ?, ?, ?)
                """, review.reviewId(), review.ticketId(), review.score(), review.comment(),
                Timestamp.from(review.createdAt()));
        return review;
    }

    private ServiceTicket mapTicket(ResultSet rs, int rowNum) throws SQLException {
        return new ServiceTicket(rs.getLong("ticket_id"), rs.getLong("user_id"), rs.getLong("order_id"),
                rs.getString("category"), rs.getString("priority"), TicketStatus.valueOf(rs.getString("status")),
                rs.getString("assignee"), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }

    private ArbitrationCase mapArbitration(ResultSet rs, int rowNum) throws SQLException {
        return new ArbitrationCase(rs.getLong("arbitration_id"), rs.getLong("ticket_id"),
                rs.getLong("merchant_id"), rs.getString("reason"),
                ArbitrationStatus.valueOf(rs.getString("status")), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }

    private CompensationRecord mapCompensation(ResultSet rs, int rowNum) throws SQLException {
        return new CompensationRecord(rs.getLong("compensation_id"), rs.getLong("ticket_id"),
                rs.getLong("user_id"), rs.getBigDecimal("amount"), rs.getString("reason"),
                rs.getTimestamp("created_at").toInstant());
    }
}
