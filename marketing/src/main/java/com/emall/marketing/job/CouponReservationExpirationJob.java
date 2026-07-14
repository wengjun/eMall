package com.emall.marketing.job;

import com.emall.common.task.DistributedTaskLock;
import com.emall.marketing.service.MarketingService;
import java.time.Duration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CouponReservationExpirationJob {
    private final MarketingService marketingService;
    private final DistributedTaskLock taskLock;

    public CouponReservationExpirationJob(MarketingService marketingService, DistributedTaskLock taskLock) {
        this.marketingService = marketingService;
        this.taskLock = taskLock;
    }

    @Scheduled(fixedDelay = 10000)
    public void releaseExpiredReservations() {
        taskLock.executeIfAcquired("marketing.coupon.release-expired", Duration.ofSeconds(30),
                () -> marketingService.releaseExpiredReservations(200));
    }
}
