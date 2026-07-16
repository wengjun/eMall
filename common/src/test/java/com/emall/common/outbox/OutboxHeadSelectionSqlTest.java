package com.emall.common.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class OutboxHeadSelectionSqlTest {
    private JdbcTemplate jdbc;

    @BeforeEach
    void createSchema() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:outbox-head;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE", "sa", "");
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("DROP TABLE IF EXISTS outbox_event");
        jdbc.execute("""
                CREATE TABLE outbox_event (
                    event_id VARCHAR(128) PRIMARY KEY,
                    aggregate_type VARCHAR(64) NOT NULL,
                    aggregate_id VARCHAR(128) NOT NULL,
                    aggregate_version BIGINT NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    next_retry_at TIMESTAMP NOT NULL,
                    claimed_until TIMESTAMP NULL,
                    shard_id INT NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
    }

    @Test
    void shouldSelectOneHeadPerAggregateWithoutHotAggregateStarvation() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 7, 15, 0, 0);
        for (int version = 1; version <= 100; version++) {
            insert("hot-" + version, "Order", "hot", version, "NEW", now.minusSeconds(1), null, 0,
                    now.plusNanos(version));
        }
        insert("cold-1", "Order", "cold", 1, "NEW", now.minusSeconds(1), null, 0, now);

        assertThat(selectHeads(now, 2)).containsExactlyInAnyOrder("cold-1", "hot-1");

        jdbc.update("UPDATE outbox_event SET status = 'PUBLISHED' WHERE event_id = 'hot-1'");
        assertThat(selectHeads(now, 2)).containsExactlyInAnyOrder("cold-1", "hot-2");
    }

    @Test
    void shouldBlockLaterEventUntilFailedOrLeasedHeadBecomesTerminal() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 7, 15, 0, 0);
        insert("failed-1", "Order", "blocked", 1, "FAILED", now.plusMinutes(5), null, 0, now.minusSeconds(2));
        insert("blocked-2", "Order", "blocked", 2, "NEW", now.minusSeconds(1), null, 0, now.minusSeconds(1));
        insert("leased-1", "Order", "leased", 1, "PROCESSING", now.minusSeconds(1), now.plusMinutes(5), 0,
                now.minusSeconds(2));
        insert("leased-2", "Order", "leased", 2, "NEW", now.minusSeconds(1), null, 0, now.minusSeconds(1));

        assertThat(selectHeads(now, 10)).isEmpty();

        jdbc.update("UPDATE outbox_event SET status = 'DEAD' WHERE event_id IN ('failed-1', 'leased-1')");
        assertThat(selectHeads(now, 10)).containsExactlyInAnyOrder("blocked-2", "leased-2");
    }

    @Test
    void shouldOrderLegacyZeroVersionEventsByCreationTimeAndEventId() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 7, 15, 0, 0);
        insert("legacy-b", "Order", "legacy", 0, "NEW", now.minusSeconds(1), null, 0, now);
        insert("legacy-a", "Order", "legacy", 0, "NEW", now.minusSeconds(1), null, 0, now);

        assertThat(selectHeads(now, 10)).containsExactly("legacy-a");
    }

    private void insert(String eventId, String aggregateType, String aggregateId, long version, String status,
            LocalDateTime nextRetryAt, LocalDateTime claimedUntil, int shardId, LocalDateTime createdAt) {
        jdbc.update("INSERT INTO outbox_event VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", eventId, aggregateType, aggregateId,
                version, status, Timestamp.valueOf(nextRetryAt),
                claimedUntil == null ? null : Timestamp.valueOf(claimedUntil), shardId, Timestamp.valueOf(createdAt));
    }

    private List<String> selectHeads(LocalDateTime now, int limit) throws Exception {
        Method method = OutboxEventMapper.class.getMethod("selectPublishableHeads", LocalDateTime.class, int.class);
        String sql = String.join("\n", method.getAnnotation(Select.class).value()).replace("#{now}", "?")
                .replace("#{limit}", "?");
        return jdbc.query(sql, (resultSet, row) -> resultSet.getString("event_id"), Timestamp.valueOf(now),
                Timestamp.valueOf(now), limit);
    }
}
