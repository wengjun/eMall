package com.emall.common.region;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emall.common.exception.BusinessException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OwnershipGuardTest {
    @Test
    void shouldAcceptOwnerRegionAndCell() {
        OwnershipGuard guard = new OwnershipGuard(properties("cn-east-1", "cell-a", RegionWriteStatus.ACTIVE));

        OwnershipDecision decision = guard.checkWrite("order", 2L);

        assertThat(decision.accepted()).isTrue();
        assertThat(decision.ownerRegion()).isEqualTo("cn-east-1");
        assertThat(decision.ownerCell()).isEqualTo("cell-a");
    }

    @Test
    void shouldRejectNonOwnerRegion() {
        OwnershipGuard guard = new OwnershipGuard(properties("cn-south-1", "cell-a", RegionWriteStatus.ACTIVE));

        assertThatThrownBy(() -> guard.checkWrite("order", 2L)).isInstanceOf(BusinessException.class)
                .hasMessageContaining("ownerRegion=cn-east-1");
    }

    @Test
    void shouldRejectReadOnlyRegion() {
        OwnershipGuard guard = new OwnershipGuard(properties("cn-east-1", "cell-a", RegionWriteStatus.READ_ONLY));

        assertThatThrownBy(() -> guard.checkWrite("order", 2L)).isInstanceOf(BusinessException.class)
                .hasMessageContaining("READ_ONLY");
    }

    private OwnershipProperties properties(String currentRegion, String currentCell, RegionWriteStatus status) {
        OwnershipProperties properties = new OwnershipProperties();
        properties.setEnabled(true);
        properties.setCurrentRegion(currentRegion);
        properties.setCurrentCell(currentCell);
        properties.setRegionStatuses(Map.of(currentRegion, status));
        OwnershipProperties.DomainOwnership order = new OwnershipProperties.DomainOwnership();
        order.setStrategy(WriteOwnershipStrategy.GLOBAL_SINGLE_WRITER);
        order.setPrimaryRegion("cn-east-1");
        order.setOwnerRegions(List.of("cn-east-1"));
        order.setOwnerCells(List.of("cell-a"));
        properties.setDomains(Map.of("order", order));
        return properties;
    }
}
