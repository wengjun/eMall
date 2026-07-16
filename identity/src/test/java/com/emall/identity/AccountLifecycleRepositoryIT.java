package com.emall.identity;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.emall.common.event.EventTypes;
import com.emall.common.event.OutboxEvent;
import com.emall.common.event.UserProfileLifecycleEventPayload;
import com.emall.common.outbox.OutboxEventRecord;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

@SpringBootTest(properties = {"emall.storage=jdbc", "spring.flyway.enabled=true",
        "spring.kafka.listener.auto-startup=false", "spring.task.scheduling.enabled=false",
        "emall.events.outbox-publish-delay=1h", "emall.identity.lifecycle.reconciliation-delay=1h"})
@EnabledIf("dockerIsAvailable")
class AccountLifecycleRepositoryIT {
    private static final String PASSWORD = "StrongPassword123";
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4").withDatabaseName("emall_identity")
            .withUsername("emall").withPassword("emall").withStartupTimeout(Duration.ofMinutes(2));

    @Autowired
    private AccountLifecycleService lifecycleService;

    @Autowired
    private IdentityAccountMapper accountMapper;

    @Autowired
    private IdentityCredentialMapper credentialMapper;

    @Autowired
    private IdentityLifecycleMapper lifecycleMapper;

    @Autowired
    private IdentityOutboxEventMapper outboxMapper;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> mysql().getJdbcUrl());
        registry.add("spring.datasource.username", () -> mysql().getUsername());
        registry.add("spring.datasource.password", () -> mysql().getPassword());
    }

    @AfterAll
    static void stopMysql() {
        MYSQL.stop();
    }

    static boolean dockerIsAvailable() {
        return DockerIntegrationSupport.isDockerAvailable();
    }

    @Test
    void concurrentRegistrationRetriesConvergeToOneAtomicIdentity() throws Exception {
        String registrationId = "mysql-registration-concurrent";
        String mobile = "13800000101";
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Long> first = executor.submit(() -> registerWithRetry(start, registrationId, mobile));
            Future<Long> second = executor.submit(() -> registerWithRetry(start, registrationId, mobile));
            start.countDown();
            long firstId = first.get();
            long secondId = second.get();

            assertThat(firstId).isEqualTo(secondId);
            assertThat(accountMapper.selectCount(new QueryWrapper<IdentityAccount>().eq("subject", mobile)))
                    .isEqualTo(1L);
            assertThat(credentialMapper.selectCount(new QueryWrapper<IdentityCredential>().eq("account_id", firstId)))
                    .isEqualTo(1L);
            assertThat(lifecycleMapper
                    .selectCount(new QueryWrapper<IdentityLifecycle>().eq("registration_id", registrationId)))
                    .isEqualTo(1L);
            assertThat(outboxMapper
                    .selectCount(new QueryWrapper<OutboxEventRecord>().eq("aggregate_id", Long.toString(firstId))))
                    .isEqualTo(1L);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void profileAcknowledgementAtomicallyActivatesIdentityAndAppendsNextEvent() {
        AccountRegistration registration =
                lifecycleService.register("mysql-registration-activation", "13800000102", "MySQL User", PASSWORD);
        IdentityLifecycle lifecycle = lifecycleMapper.selectById(registration.accountId());
        List<OutboxEventRecord> events = outboxMapper.selectList(new QueryWrapper<OutboxEventRecord>()
                .eq("aggregate_id", Long.toString(registration.accountId())).orderByAsc("aggregate_version"));
        OutboxEvent acknowledgement = OutboxEvent.create("mysql-profile-ready", "UserProfile",
                Long.toString(registration.accountId()), EventTypes.USER_PROFILE_READY, "user", "1.0.0",
                new UserProfileLifecycleEventPayload(registration.accountId(), lifecycle.bindingHash(), "NORMAL",
                        events.get(0).getAggregateVersion()))
                .withAggregateVersion(1L);

        lifecycleService.handleProfileEvent(acknowledgement);

        assertThat(accountMapper.selectById(registration.accountId()).status()).isEqualTo(IdentityStatus.ACTIVE);
        assertThat(lifecycleMapper.selectById(registration.accountId()).projectionStatus())
                .isEqualTo(ProfileProjectionStatus.READY);
        assertThat(outboxMapper.selectCount(
                new QueryWrapper<OutboxEventRecord>().eq("aggregate_id", Long.toString(registration.accountId()))))
                .isEqualTo(2L);
    }

    private long registerWithRetry(CountDownLatch start, String registrationId, String mobile) throws Exception {
        start.await();
        RuntimeException lastFailure = null;
        for (int attempt = 0; attempt < 5; attempt++) {
            try {
                return lifecycleService.register(registrationId, mobile, "Concurrent User", PASSWORD).accountId();
            } catch (RuntimeException exception) {
                lastFailure = exception;
                Thread.sleep(50L * (attempt + 1));
            }
        }
        throw lastFailure;
    }

    private static MySQLContainer<?> mysql() {
        if (!MYSQL.isRunning()) {
            MYSQL.start();
        }
        return MYSQL;
    }
}
