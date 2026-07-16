package com.emall.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import com.emall.common.sharding.ShardRouteRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        properties = {"spring.datasource.url=jdbc:h2:mem:routing;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                "spring.datasource.username=sa", "spring.datasource.password=",
                "spring.datasource.driver-class-name=org.h2.Driver", "spring.flyway.enabled=true",
                "spring.cloud.nacos.discovery.enabled=false", "spring.cloud.nacos.config.enabled=false",
                "emall.security.auth.enabled=false"})
class ShardRoutePersistenceTest {
    private static final String FIRST_HASH = "a".repeat(64);
    private static final String SECOND_HASH = "b".repeat(64);
    private static final String THIRD_HASH = "c".repeat(64);

    @Autowired
    private ShardRouteService service;

    @Test
    void persistsRoutesAndUsesOptimisticVersionsForMutationFencing() {
        ShardRouteRecord created = service.bind("order-id", FIRST_HASH, 1001L, null, true);
        ShardRouteRecord updated = service.bind("order-id", FIRST_HASH, 1001L, null, true);

        assertThat(service.resolve("order-id", FIRST_HASH)).contains(updated);
        assertThat(created.version()).isEqualTo(1L);
        assertThat(updated.version()).isEqualTo(2L);
        assertThat(service.removeIfOwned("order-id", FIRST_HASH, 1001L, created.version())).isFalse();
        assertThat(service.removeIfOwned("order-id", FIRST_HASH, 1001L, updated.version())).isTrue();
    }

    @Test
    void scansWithAStableKeysetCursorAndRejectsOwnershipChanges() {
        service.bind("order-id", SECOND_HASH, 1001L, null, true);
        service.bind("order-id", THIRD_HASH, 1002L, null, true);

        var firstPage = service.scan(null, 1);
        var secondPage = service.scan(firstPage.nextCursor(), 1);

        assertThat(firstPage.routes()).hasSize(1);
        assertThat(firstPage.nextCursor()).isNotBlank();
        assertThat(secondPage.routes()).hasSize(1);
        assertThat(secondPage.nextCursor()).isNull();
        assertThatThrownBy(() -> service.bind("order-id", SECOND_HASH, 2002L, null, true)).isInstanceOfSatisfying(
                BusinessException.class, exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.CONFLICT));
    }
}
