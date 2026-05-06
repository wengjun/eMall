package com.emall.user.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.emall.user.domain.UserAccount;
import com.emall.user.domain.UserStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = "emall.storage=jdbc")
@Testcontainers(disabledWithoutDocker = true)
class UserRepositoryIntegrationTest {
    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("emall_user")
            .withUsername("emall")
            .withPassword("emall");

    @Autowired
    private UserRepository userRepository;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Test
    void savesAndLoadsUserAccountThroughJdbcAndFlyway() {
        Instant now = Instant.now();
        UserAccount user = new UserAccount(1L, "15500000000", "tester", UserStatus.NORMAL, now, now);

        userRepository.save(user);

        assertThat(userRepository.findByMobile("15500000000"))
                .isPresent()
                .get()
                .extracting(UserAccount::userId)
                .isEqualTo(1L);
    }
}
