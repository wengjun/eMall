package com.emall.search.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emall.common.event.EventTypes;
import com.emall.common.event.OutboxEvent;
import com.emall.common.metrics.BusinessMetrics;
import com.emall.search.domain.SearchDocument;
import com.emall.search.repository.InMemoryProcessedMessageRepository;
import com.emall.search.repository.InMemorySearchRepository;
import com.emall.search.service.SearchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ProductEventConsumerTest {
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final InMemoryProcessedMessageRepository processedMessages = new InMemoryProcessedMessageRepository();

    @Test
    void skipsDuplicateProductEventsAfterSuccessfulProcessing() throws Exception {
        CountingSearchService searchService = new CountingSearchService();
        ProductEventConsumer consumer =
                new ProductEventConsumer(objectMapper, searchService, processedMessages, BusinessMetrics.noop(), 4);
        String message = productChangedMessage("event-1", 30001L, "phone pro", 1L);

        consumer.onProductEvent(message);
        consumer.onProductEvent(message);

        assertThat(searchService.indexCount).isEqualTo(1);
        assertThat(searchService.get(30001L).title()).isEqualTo("phone pro");
    }

    @Test
    void allowsRetryAfterFailedProcessing() throws Exception {
        FailingOnceSearchService searchService = new FailingOnceSearchService();
        ProductEventConsumer consumer =
                new ProductEventConsumer(objectMapper, searchService, processedMessages, BusinessMetrics.noop(), 4);
        String message = productChangedMessage("event-2", 30002L, "retry phone", 2L);

        assertThatThrownBy(() -> consumer.onProductEvent(message)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("temporary index failure");
        consumer.onProductEvent(message);

        assertThat(searchService.indexCount).isEqualTo(2);
        assertThat(searchService.get(30002L).title()).isEqualTo("retry phone");
    }

    @Test
    void reclaimsExpiredProcessingMessageAfterConsumerCrash() {
        MutableClock clock = new MutableClock(Instant.parse("2026-05-19T00:00:00Z"));
        InMemoryProcessedMessageRepository repository = new InMemoryProcessedMessageRepository(clock);

        assertThat(repository.markProcessing("event-crash")).isTrue();
        assertThat(repository.markProcessing("event-crash")).isFalse();

        clock.advance(Duration.ofMinutes(6));

        assertThat(repository.markProcessing("event-crash")).isTrue();
    }

    private String productChangedMessage(String eventId, long skuId, String title, long version) throws Exception {
        OutboxEvent event = OutboxEvent.create(eventId, "Product", String.valueOf(skuId), EventTypes.PRODUCT_CHANGED,
                Map.of("skuId", skuId, "title", title, "category", "digital", "price", new BigDecimal("3999.00"),
                        "saleable", true, "version", version));
        return objectMapper.writeValueAsString(event);
    }

    private static class CountingSearchService extends SearchService {
        private int indexCount;

        CountingSearchService() {
            super(new InMemorySearchRepository());
        }

        @Override
        public SearchDocument index(long skuId, String title, String category, BigDecimal price, Set<String> tags,
                boolean saleable, long version) {
            indexCount++;
            return super.index(skuId, title, category, price, tags, saleable, version);
        }
    }

    private static class FailingOnceSearchService extends SearchService {
        private int indexCount;

        FailingOnceSearchService() {
            super(new InMemorySearchRepository());
        }

        @Override
        public SearchDocument index(long skuId, String title, String category, BigDecimal price, Set<String> tags,
                boolean saleable, long version) {
            indexCount++;
            if (indexCount == 1) {
                throw new IllegalStateException("temporary index failure");
            }
            return super.index(skuId, title, category, price, tags, saleable, version);
        }
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        private MutableClock(Instant now) {
            this.now = now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }

        private void advance(Duration duration) {
            now = now.plus(duration);
        }
    }
}
