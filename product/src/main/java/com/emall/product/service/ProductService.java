package com.emall.product.service;

import com.emall.common.api.ErrorCode;
import com.emall.common.event.EventTypes;
import com.emall.common.event.OutboxEvent;
import com.emall.common.exception.BusinessException;
import com.emall.common.id.SnowflakeIdGenerator;
import com.emall.common.outbox.OutboxRepository;
import com.emall.product.domain.Product;
import com.emall.product.domain.ProductStatus;
import com.emall.product.repository.ProductRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final OutboxRepository outboxRepository;
    private final SnowflakeIdGenerator idGenerator;

    public ProductService(ProductRepository productRepository,
                          OutboxRepository outboxRepository,
                          SnowflakeIdGenerator idGenerator) {
        this.productRepository = productRepository;
        this.outboxRepository = outboxRepository;
        this.idGenerator = idGenerator;
    }

    @Caching(evict = @CacheEvict(value = "productSearch", allEntries = true))
    @Transactional
    public Product create(long spuId, String title, String category, BigDecimal price) {
        Instant now = Instant.now();
        Product product = new Product(idGenerator.nextId(), spuId, title, category, price,
                ProductStatus.DRAFT, now, now);
        Product saved = productRepository.save(product);
        appendProductChanged(saved);
        return saved;
    }

    @Cacheable(value = "products", key = "#skuId")
    public Product get(long skuId) {
        return productRepository.findBySkuId(skuId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "product not found"));
    }

    @Cacheable(value = "productSearch", key = "(#keyword == null ? '' : #keyword) + ':' + #limit")
    public List<Product> search(String keyword, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        return productRepository.search(keyword, safeLimit);
    }

    @Caching(
            put = @CachePut(value = "products", key = "#skuId"),
            evict = @CacheEvict(value = "productSearch", allEntries = true)
    )
    @Transactional
    public Product changePrice(long skuId, BigDecimal price) {
        if (price.signum() <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "price must be positive");
        }
        Product saved = productRepository.save(get(skuId).changePrice(price));
        appendProductChanged(saved);
        return saved;
    }

    @Caching(
            put = @CachePut(value = "products", key = "#skuId"),
            evict = @CacheEvict(value = "productSearch", allEntries = true)
    )
    @Transactional
    public Product changeStatus(long skuId, ProductStatus status) {
        Product saved = productRepository.save(get(skuId).changeStatus(status));
        appendProductChanged(saved);
        return saved;
    }

    @Caching(
            put = @CachePut(value = "products", key = "#skuId"),
            evict = @CacheEvict(value = "productSearch", allEntries = true)
    )
    @Transactional
    public Product rename(long skuId, String title) {
        Product saved = productRepository.save(get(skuId).rename(title));
        appendProductChanged(saved);
        return saved;
    }

    private void appendProductChanged(Product product) {
        outboxRepository.save(OutboxEvent.create(
                "product-event-" + idGenerator.nextId(),
                "Product",
                String.valueOf(product.skuId()),
                EventTypes.PRODUCT_CHANGED,
                Map.of(
                        "skuId", product.skuId(),
                        "spuId", product.spuId(),
                        "title", product.title(),
                        "category", product.category(),
                        "price", product.price(),
                        "status", product.status().name(),
                        "saleable", product.status() == ProductStatus.ON_SALE
                )));
    }
}
