package com.emall.marketing.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emall.common.task.PartitionedShardWorkCoordinator;
import com.emall.common.task.PartitionedShardWorkCoordinator.PartitionLease;
import com.emall.marketing.service.MarketingService;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class CouponReservationExpirationJobTest {
    @Test
    void shouldReleaseCouponsOnlyInsideABoundedPartitionLease() {
        MarketingService marketingService = mock(MarketingService.class);
        PartitionedShardWorkCoordinator workCoordinator = mock(PartitionedShardWorkCoordinator.class);
        PartitionLease lease = mock(PartitionLease.class);
        when(marketingService.releaseExpiredReservations(25)).thenReturn(3);
        when(workCoordinator.execute(eq("marketing.coupon.release-expired"), eq("coupon"), eq(8),
                eq(Duration.ofSeconds(30)), any()))
                .thenAnswer(invocation -> invocation
                        .getArgument(4, PartitionedShardWorkCoordinator.ShardPartitionTask.class).run(lease));
        CouponReservationExpirationJob job =
                new CouponReservationExpirationJob(marketingService, workCoordinator, 8, Duration.ofSeconds(30));

        assertThat(job.releaseExpiredReservations(200)).isEqualTo(3);
        verify(lease).requireValid();
        verify(marketingService).releaseExpiredReservations(25);
    }
}
