package com.emall.payment.job;

import com.emall.common.task.DistributedTaskLock;
import com.emall.payment.service.PaymentService;
import java.time.Duration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PaymentCompensationJob {
    private static final Duration LOCK_TTL = Duration.ofSeconds(30);

    private final PaymentService paymentService;
    private final DistributedTaskLock taskLock;

    public PaymentCompensationJob(PaymentService paymentService, DistributedTaskLock taskLock) {
        this.paymentService = paymentService;
        this.taskLock = taskLock;
    }

    @Scheduled(fixedDelay = 5000)
    public void retryOrderConfirmation() {
        retryOrderConfirmation(100);
    }

    public int retryOrderConfirmation(int limit) {
        return taskLock.executeIfAcquired("payment.compensation.retry-order-confirmation",
                LOCK_TTL, () -> retryOrderConfirmationUnlocked(limit));
    }

    private int retryOrderConfirmationUnlocked(int limit) {
        return paymentService.findSucceededButUnconfirmed(limit).stream()
                .map(payment -> paymentService.retryOrderConfirmation(payment.paymentId()))
                .toList()
                .size();
    }

    @Scheduled(fixedDelay = 30000)
    public void reconcileChannelStatements() {
        reconcileChannelStatements(100);
    }

    public int reconcileChannelStatements(int limit) {
        return taskLock.executeIfAcquired("payment.reconciliation.channel-statements",
                LOCK_TTL, () -> paymentService.reconcileChannelStatements(limit));
    }
}
