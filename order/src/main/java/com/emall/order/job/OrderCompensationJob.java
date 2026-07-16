package com.emall.order.job;

import com.emall.common.task.PartitionedShardWorkCoordinator;
import com.emall.order.domain.Order;
import com.emall.order.domain.OrderStatus;
import com.emall.order.service.OrderService;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OrderCompensationJob {
    private static final String TASK_NAME = "order.compensation.retry-pending";

    private final OrderService orderService;
    private final PartitionedShardWorkCoordinator workCoordinator;
    private final int maxPartitionsPerRun;
    private final Duration leaseTtl;

    public OrderCompensationJob(OrderService orderService, PartitionedShardWorkCoordinator workCoordinator,
            @Value("${emall.jobs.order-compensation.max-partitions-per-run:8}") int maxPartitionsPerRun,
            @Value("${emall.jobs.order-compensation.lease-ttl:30s}") Duration leaseTtl) {
        this.orderService = orderService;
        this.workCoordinator = workCoordinator;
        this.maxPartitionsPerRun = maxPartitionsPerRun;
        this.leaseTtl = leaseTtl;
    }

    @Scheduled(fixedDelay = 5000)
    public void retryPendingOrders() {
        retryPendingOrders(100);
    }

    public int retryPendingOrders(int limit) {
        int boundedLimit = Math.max(1, Math.min(limit, 1000));
        int perPartitionLimit = Math.max(1, (boundedLimit + maxPartitionsPerRun - 1) / maxPartitionsPerRun);
        AtomicInteger remaining = new AtomicInteger(boundedLimit);
        return workCoordinator.execute(TASK_NAME, "order_record", maxPartitionsPerRun, leaseTtl, lease -> {
            int requested = Math.min(perPartitionLimit, remaining.get());
            if (requested <= 0) {
                return 0;
            }
            int processed = 0;
            for (Order order : orderService.findByStatus(OrderStatus.PENDING_RETRY, requested)) {
                lease.requireValid();
                if (remaining.get() <= 0) {
                    break;
                }
                remaining.decrementAndGet();
                orderService.retryPending(order.orderId());
                processed++;
            }
            return processed;
        });
    }
}
