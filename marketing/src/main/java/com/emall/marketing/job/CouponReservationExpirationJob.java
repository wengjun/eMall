package com.emall.marketing.job;

import com.emall.common.task.PartitionedShardWorkCoordinator;
import com.emall.marketing.service.MarketingService;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CouponReservationExpirationJob {
    private final MarketingService marketingService;
    private final PartitionedShardWorkCoordinator workCoordinator;
    private final int maxPartitionsPerRun;
    private final Duration leaseTtl;

    public CouponReservationExpirationJob(MarketingService marketingService,
            PartitionedShardWorkCoordinator workCoordinator,
            @Value("${emall.jobs.coupon-expiration.max-partitions-per-run:8}") int maxPartitionsPerRun,
            @Value("${emall.jobs.coupon-expiration.lease-ttl:30s}") Duration leaseTtl) {
        this.marketingService = marketingService;
        this.workCoordinator = workCoordinator;
        this.maxPartitionsPerRun = maxPartitionsPerRun;
        this.leaseTtl = leaseTtl;
    }

    @Scheduled(fixedDelay = 10000)
    public void releaseExpiredReservations() {
        releaseExpiredReservations(200);
    }

    public int releaseExpiredReservations(int limit) {
        int boundedLimit = Math.max(1, Math.min(limit, 1000));
        int perPartitionLimit = Math.max(1, (boundedLimit + maxPartitionsPerRun - 1) / maxPartitionsPerRun);
        AtomicInteger remaining = new AtomicInteger(boundedLimit);
        return workCoordinator.execute("marketing.coupon.release-expired", "coupon", maxPartitionsPerRun, leaseTtl,
                lease -> {
                    lease.requireValid();
                    int requested = Math.min(perPartitionLimit, remaining.get());
                    if (requested <= 0) {
                        return 0;
                    }
                    int processed = marketingService.releaseExpiredReservations(requested);
                    remaining.addAndGet(-processed);
                    return processed;
                });
    }
}
