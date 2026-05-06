package com.emall.order.job;

import com.emall.common.task.DistributedTaskLock;
import com.emall.order.domain.Order;
import com.emall.order.domain.OrderStatus;
import com.emall.order.service.OrderService;
import java.time.Duration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OrderCompensationJob {
    private static final String LOCK_NAME = "order.compensation.retry-pending";
    private static final Duration LOCK_TTL = Duration.ofSeconds(30);

    private final OrderService orderService;
    private final DistributedTaskLock taskLock;

    public OrderCompensationJob(OrderService orderService, DistributedTaskLock taskLock) {
        this.orderService = orderService;
        this.taskLock = taskLock;
    }

    @Scheduled(fixedDelay = 5000)
    public void retryPendingOrders() {
        retryPendingOrders(100);
    }

    public int retryPendingOrders(int limit) {
        return taskLock.executeIfAcquired(LOCK_NAME, LOCK_TTL, () -> retryPendingOrdersUnlocked(limit));
    }

    private int retryPendingOrdersUnlocked(int limit) {
        int retried = 0;
        for (Order order : orderService.findByStatus(OrderStatus.PENDING_RETRY, limit)) {
            orderService.retryPending(order.orderId());
            retried++;
        }
        return retried;
    }
}
