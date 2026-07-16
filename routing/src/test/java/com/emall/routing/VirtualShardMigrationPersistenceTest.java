package com.emall.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.emall.common.exception.BusinessException;
import com.emall.common.sharding.PhysicalShardPlacement;
import com.emall.common.sharding.ShardMigrationState;
import com.emall.common.sharding.ShardWriteFencedException;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:virtual-shard;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.flyway.enabled=true",
        "spring.cloud.nacos.discovery.enabled=false", "spring.cloud.nacos.config.enabled=false",
        "emall.security.auth.enabled=false", "emall.routing.virtual-shard-migration.mapping-cache-ttl=1ms",
        "emall.routing.virtual-shard-migration.minimum-cutover-delay=2ms",
        "emall.routing.virtual-shard-migration.observation-period=1ms"})
class VirtualShardMigrationPersistenceTest {
    @Autowired
    private VirtualShardMigrationService service;
    @Autowired
    private VirtualShardMigrationAuditMapper auditMapper;

    @Test
    void enforcesEvidenceGatesAndCompletesVersionedCutover() throws InterruptedException {
        PhysicalShardPlacement source = placement("emall_order_00", 0, "cell-a", "orders_00");
        PhysicalShardPlacement target = placement("emall_order_08", 8, "cell-b", "orders_00");

        var preparing = service.start("order", 42, source, target, 1L, "capacity-controller");
        var copying = service.advance("order", 42, preparing.mappingVersion(), ShardMigrationState.COPYING, null,
                "capacity-controller");
        var catchingUp = service.advance("order", 42, copying.mappingVersion(), ShardMigrationState.CATCHING_UP,
                evidence("cursor-100", 100L, 95L, null, null, 5L), "capacity-controller");
        var verifying = service.advance("order", 42, catchingUp.mappingVersion(), ShardMigrationState.VERIFYING,
                evidence(null, null, 100L, null, null, 0L), "capacity-controller");

        assertThatThrownBy(
                () -> service.advance("order", 42, verifying.mappingVersion(), ShardMigrationState.CUTOVER_PENDING,
                        evidence(null, null, null, "source", "target", 0L), "capacity-controller"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("checksums");

        var fenced = service.advance("order", 42, verifying.mappingVersion(), ShardMigrationState.CUTOVER_PENDING,
                evidence(null, null, null, "same", "same", 0L), "capacity-controller");
        assertThatThrownBy(fenced::requireWriteAllowed).isInstanceOf(ShardWriteFencedException.class);

        Thread.sleep(Duration.ofMillis(10).toMillis());
        var observing = service.advance("order", 42, fenced.mappingVersion(), ShardMigrationState.OBSERVING, null,
                "capacity-controller");
        assertThat(observing.primary()).isEqualTo(target);
        assertThat(observing.migrationTarget()).isEqualTo(source);
        assertThat(observing.epoch()).isEqualTo(2L);

        Thread.sleep(Duration.ofMillis(10).toMillis());
        var cleanup = service.advance("order", 42, observing.mappingVersion(), ShardMigrationState.CLEANUP, null,
                "capacity-controller");
        var stable = service.advance("order", 42, cleanup.mappingVersion(), ShardMigrationState.STABLE, null,
                "capacity-controller");

        assertThat(stable.state()).isEqualTo(ShardMigrationState.STABLE);
        assertThat(stable.primary()).isEqualTo(target);
        assertThat(stable.migrationTarget()).isNull();
        assertThat(service.resolve("order", 42)).contains(stable);
        assertThat(auditMapper
                .selectCount(new QueryWrapper<VirtualShardMigrationAuditEntity>().eq("placement_id", "order:42")))
                .isEqualTo(8L);
    }

    @Test
    void rollsBackBeforeCutoverWithoutChangingEpoch() {
        PhysicalShardPlacement source = placement("emall_payment_00", 0, "cell-a", "payments_00");
        PhysicalShardPlacement target = placement("emall_payment_08", 8, "cell-b", "payments_00");

        var preparing = service.start("payment", 7, source, target, 1L, "capacity-controller");
        var rolledBack = service.rollback("payment", 7, preparing.mappingVersion(), "capacity-controller");

        assertThat(rolledBack.state()).isEqualTo(ShardMigrationState.ROLLED_BACK);
        assertThat(rolledBack.primary()).isEqualTo(source);
        assertThat(rolledBack.migrationTarget()).isNull();
        assertThat(rolledBack.epoch()).isEqualTo(1L);
    }

    private VirtualShardMigrationEvidence evidence(String cursor, Long sourceRows, Long targetRows,
            String sourceChecksum, String targetChecksum, Long cdcLag) {
        return new VirtualShardMigrationEvidence(cursor, sourceRows, targetRows, sourceChecksum, targetChecksum,
                cdcLag);
    }

    private PhysicalShardPlacement placement(String database, int databaseIndex, String cell, String table) {
        return new PhysicalShardPlacement(database, databaseIndex, "cn-east-1", cell, Map.of("orders", table));
    }
}
