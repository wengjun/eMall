package com.emall.search.repository;

import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
public class JdbcProcessedMessageRepository implements ProcessedMessageRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcProcessedMessageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean markProcessing(String messageId) {
        try {
            jdbcTemplate.update("INSERT INTO processed_message (message_id, processed_at) VALUES (?, ?)",
                    messageId, Timestamp.from(Instant.now()));
            return true;
        } catch (DuplicateKeyException ex) {
            return false;
        }
    }
}
