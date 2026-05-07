package com.emall.openapi;

import static org.assertj.core.api.Assertions.assertThat;

import com.emall.common.id.SnowflakeIdGenerator;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class OpenApiServiceTest {
    private final InMemoryOpenApiRepository repository = new InMemoryOpenApiRepository();
    private final OpenApiService service = new OpenApiService(repository, new SnowflakeIdGenerator(24L));

    @Test
    void verifiesSignatureAndConsumesQuota() {
        AppRegistration registration = service.registerApp(1001L, "seller-api", "order:read", 2);
        long timestamp = Instant.now().getEpochSecond();
        String signature = service.signatureFixture(registration.appSecret(), registration.app().appKey(),
                "/open/orders", "nonce-1", timestamp);

        ApiSignatureVerification verification = service.verifySignature(registration.app().appKey(),
                registration.appSecret(), "/open/orders", "nonce-1", timestamp, signature);
        ApiQuotaUsage first = service.consumeQuota(registration.app().appKey());
        ApiQuotaUsage second = service.consumeQuota(registration.app().appKey());
        ApiQuotaUsage third = service.consumeQuota(registration.app().appKey());

        assertThat(verification.valid()).isTrue();
        assertThat(first.allowed()).isTrue();
        assertThat(second.allowed()).isTrue();
        assertThat(third.allowed()).isFalse();
    }

    @Test
    void recordsWebhookSubscriptionAndDelivery() {
        AppRegistration registration = service.registerApp(1001L, "seller-api", "order:read", 100);

        WebhookSubscription subscription =
                service.subscribe(registration.app().appId(), "order.paid", "https://merchant.example/webhook");
        WebhookDelivery delivery =
                service.recordDelivery(subscription.subscriptionId(), "event-1", WebhookDeliveryStatus.DELIVERED);

        assertThat(service.findSubscriptions(registration.app().appId())).hasSize(1);
        assertThat(delivery.status()).isEqualTo(WebhookDeliveryStatus.DELIVERED);
    }
}
