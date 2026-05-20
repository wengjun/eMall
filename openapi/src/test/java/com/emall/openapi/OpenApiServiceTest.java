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
    void authenticatesSignedRequestWithNonceScopeAndQuota() {
        AppRegistration registration = service.registerApp(1001L, "seller-api", "order:create,order:read", 1);
        long timestamp = Instant.now().getEpochSecond();
        String signature = service.signatureFixture(registration.appSecret(), registration.app().appKey(),
                "/open/orders", "nonce-2", timestamp);

        ApiRequestAuthentication accepted = service.authenticateRequest(registration.app().appKey(), "/open/orders",
                "nonce-2", timestamp, signature, "order:create");
        ApiRequestAuthentication replayed = service.authenticateRequest(registration.app().appKey(), "/open/orders",
                "nonce-2", timestamp, signature, "order:create");
        String secondSignature = service.signatureFixture(registration.appSecret(), registration.app().appKey(),
                "/open/orders", "nonce-3", timestamp);
        ApiRequestAuthentication quotaExceeded = service.authenticateRequest(registration.app().appKey(),
                "/open/orders", "nonce-3", timestamp, secondSignature, "order:create");

        assertThat(accepted.allowed()).isTrue();
        assertThat(replayed.reason()).isEqualTo("nonce-replayed");
        assertThat(quotaExceeded.reason()).isEqualTo("quota-exceeded");
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
