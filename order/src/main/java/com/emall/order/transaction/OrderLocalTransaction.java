package com.emall.order.transaction;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class OrderLocalTransaction {
    public static final String DURATION_METRIC = "emall_order_local_transaction_duration";
    private final TransactionTemplate transactionTemplate;
    private final MeterRegistry meterRegistry;

    @Autowired
    public OrderLocalTransaction(ObjectProvider<PlatformTransactionManager> transactionManagerProvider,
            ObjectProvider<MeterRegistry> meterRegistryProvider,
            @Value("${emall.order.local-transaction-timeout-seconds:3}") int timeoutSeconds) {
        PlatformTransactionManager transactionManager = transactionManagerProvider.getIfAvailable();
        this.transactionTemplate = transactionManager == null ? null : new TransactionTemplate(transactionManager);
        if (transactionTemplate != null) {
            transactionTemplate.setTimeout(Math.max(1, Math.min(timeoutSeconds, 30)));
        }
        this.meterRegistry = meterRegistryProvider.getIfAvailable();
    }

    private OrderLocalTransaction() {
        this.transactionTemplate = null;
        this.meterRegistry = null;
    }

    public static OrderLocalTransaction direct() {
        return new OrderLocalTransaction();
    }

    public <T> T execute(String operation, Supplier<T> action) {
        Objects.requireNonNull(operation, "operation must not be null");
        Objects.requireNonNull(action, "action must not be null");
        long startedAt = System.nanoTime();
        try {
            if (transactionTemplate == null) {
                return action.get();
            }
            return transactionTemplate.execute(status -> action.get());
        } finally {
            if (meterRegistry != null) {
                Timer.builder(DURATION_METRIC).description("Duration of short order database transactions")
                        .tag("operation", operation).register(meterRegistry)
                        .record(System.nanoTime() - startedAt, TimeUnit.NANOSECONDS);
            }
        }
    }
}
