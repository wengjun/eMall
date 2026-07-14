package com.emall.common.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emall.common.event.EventTypes;
import com.emall.common.event.OutboxEvent;
import com.emall.common.messaging.ConsumerExecutionResult;
import com.emall.common.messaging.DeadMessageException;
import com.emall.common.messaging.MessageConsumerTemplate;
import com.emall.common.messaging.ProcessedMessageRepository;
import com.emall.common.metrics.BusinessMetrics;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

@EnabledIf("dockerIsAvailable")
class MessageConsumerTransactionIT {
    private static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>(DockerImageName.parse("mysql:8.4")).withDatabaseName("message_test")
                    .withUsername("test").withPassword("test").withStartupTimeout(Duration.ofMinutes(2));
    private static DriverManagerDataSource dataSource;
    private static JdbcTemplate jdbcTemplate;

    @BeforeAll
    static void startDatabase() {
        MYSQL.start();
        dataSource = new DriverManagerDataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("""
                CREATE TABLE processed_message (
                    message_id VARCHAR(128) PRIMARY KEY,
                    status VARCHAR(32) NOT NULL,
                    retry_count INT NOT NULL,
                    last_error_code VARCHAR(128),
                    last_error VARCHAR(512),
                    updated_at TIMESTAMP(6) NOT NULL
                )
                """);
        jdbcTemplate.execute("CREATE TABLE message_side_effect (event_id VARCHAR(128) PRIMARY KEY)");
    }

    @AfterAll
    static void stopDatabase() {
        MYSQL.stop();
    }

    static boolean dockerIsAvailable() {
        return DockerIntegrationSupport.isDockerAvailable();
    }

    @Test
    void commitsFailureAndDeadStateWhileRollingBackBusinessSideEffects() throws Exception {
        MessageConsumerTemplate template = new MessageConsumerTemplate(new ObjectMapper().findAndRegisterModules(),
                new JdbcProcessedMessageRepository(jdbcTemplate), BusinessMetrics.noop(), 2, "transaction-it",
                new DataSourceTransactionManager(dataSource));
        String message = new ObjectMapper().findAndRegisterModules().writeValueAsString(OutboxEvent
                .create("event-transaction-1", "Order", "1001", EventTypes.ORDER_PAID, Map.of("orderId", 1001L)));

        assertThatThrownBy(() -> template.consume(message, EventTypes.ORDER_PAID, this::writeThenFail))
                .isInstanceOf(IllegalStateException.class);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM message_side_effect", Integer.class)).isZero();
        assertThat(status()).isEqualTo("FAILED:1");

        assertThatThrownBy(() -> template.consume(message, EventTypes.ORDER_PAID, this::writeThenFail))
                .isInstanceOf(DeadMessageException.class);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM message_side_effect", Integer.class)).isZero();
        assertThat(status()).isEqualTo("DEAD:2");
        assertThat(template.consume(message, EventTypes.ORDER_PAID, ignored -> {
        })).isEqualTo(ConsumerExecutionResult.DUPLICATED);
    }

    private void writeThenFail(OutboxEvent event) {
        jdbcTemplate.update("INSERT INTO message_side_effect(event_id) VALUES (?)", event.eventId());
        throw new IllegalStateException("injected business failure");
    }

    private String status() {
        return jdbcTemplate.queryForObject(
                "SELECT CONCAT(status, ':', retry_count) FROM processed_message WHERE message_id = ?", String.class,
                "event-transaction-1");
    }

    private static final class JdbcProcessedMessageRepository implements ProcessedMessageRepository {
        private final JdbcTemplate jdbc;

        private JdbcProcessedMessageRepository(JdbcTemplate jdbc) {
            this.jdbc = jdbc;
        }

        @Override
        public boolean markProcessing(String messageId) {
            try {
                jdbc.update("INSERT INTO processed_message VALUES (?, 'PROCESSING', 0, NULL, NULL, ?)", messageId,
                        LocalDateTime.now());
                return true;
            } catch (org.springframework.dao.DuplicateKeyException ex) {
                return jdbc.update("UPDATE processed_message SET status = 'PROCESSING', updated_at = ? "
                        + "WHERE message_id = ? AND status = 'FAILED'", LocalDateTime.now(), messageId) == 1;
            }
        }

        @Override
        public void markProcessed(String messageId) {
            jdbc.update("UPDATE processed_message SET status = 'PROCESSED', updated_at = ? WHERE message_id = ?",
                    LocalDateTime.now(), messageId);
        }

        @Override
        public int markFailed(String messageId, String errorCode, String lastError) {
            int updated = jdbc.update(
                    "UPDATE processed_message SET status = 'FAILED', retry_count = retry_count + 1, "
                            + "last_error_code = ?, last_error = ?, updated_at = ? WHERE message_id = ?",
                    errorCode, lastError, LocalDateTime.now(), messageId);
            if (updated == 0) {
                jdbc.update("INSERT INTO processed_message VALUES (?, 'FAILED', 1, ?, ?, ?)", messageId, errorCode,
                        lastError, LocalDateTime.now());
            }
            return jdbc.queryForObject("SELECT retry_count FROM processed_message WHERE message_id = ?", Integer.class,
                    messageId);
        }

        @Override
        public void markDead(String messageId, String errorCode, String lastError) {
            jdbc.update(
                    "UPDATE processed_message SET status = 'DEAD', last_error_code = ?, last_error = ?, "
                            + "updated_at = ? WHERE message_id = ?",
                    errorCode, lastError, LocalDateTime.now(), messageId);
        }
    }
}
