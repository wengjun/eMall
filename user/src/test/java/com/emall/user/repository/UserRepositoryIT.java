package com.emall.user.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.emall.user.domain.UserAccount;
import com.emall.user.domain.UserStatus;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

@SpringBootTest(properties = "emall.storage=jdbc")
@EnabledIf("dockerIsAvailable")
class UserRepositoryIT {
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4").withDatabaseName("emall_user")
            .withUsername("emall").withPassword("emall").withStartupTimeout(Duration.ofMinutes(2));

    @Autowired
    private UserRepository userRepository;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> mysql().getJdbcUrl());
        registry.add("spring.datasource.username", () -> mysql().getUsername());
        registry.add("spring.datasource.password", () -> mysql().getPassword());
    }

    @AfterAll
    static void stopMysql() {
        mysql.stop();
    }

    static boolean dockerIsAvailable() {
        return DockerIntegrationSupport.isDockerAvailable();
    }

    @Test
    void savesAndLoadsUserAccountThroughMybatisPlusAndFlyway() {
        Instant now = Instant.now();
        UserAccount user = new UserAccount(1L, "15500000000", "tester", UserStatus.NORMAL, now, now);

        userRepository.save(user);

        assertThat(userRepository.findByMobile("15500000000")).isPresent().get().extracting(UserAccount::userId)
                .isEqualTo(1L);
    }

    private static MySQLContainer<?> mysql() {
        if (!mysql.isRunning()) {
            mysql.start();
        }
        return mysql;
    }
}
