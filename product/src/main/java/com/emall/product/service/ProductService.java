package com.emall.product.service;

import com.emall.common.api.ErrorCode;
import com.emall.common.cache.CacheTtlPolicy;
import com.emall.common.cache.TwoLevelCache;
import com.emall.common.event.EventTypes;
import com.emall.common.event.OutboxEvent;
import com.emall.common.exception.BusinessException;
import com.emall.common.id.SnowflakeIdGenerator;
import com.emall.common.outbox.OutboxRepository;
import com.emall.common.region.OwnershipGuard;
import com.emall.common.sharding.ShardRoutingOperations;
import com.emall.product.domain.Product;
import com.emall.product.domain.ProductStatus;
import com.emall.product.repository.ProductRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final OutboxRepository outboxRepository;
    private final SnowflakeIdGenerator idGenerator;
    private final ShardRoutingOperations shardRoutingOperations;
    private final OwnershipGuard ownershipGuard;
    private final TwoLevelCache<Long, ProductCacheEntry> productDetailCache;
    private final CacheTtlPolicy productTtlPolicy = new CacheTtlPolicy(Duration.ofMinutes(10), 0.1);
    private final CacheTtlPolicy nullTtlPolicy = new CacheTtlPolicy(Duration.ofSeconds(30), 0.2);

    public ProductService(ProductRepository productRepository, OutboxRepository outboxRepository,
            SnowflakeIdGenerator idGenerator) {
        this.productRepository = productRepository;
        this.outboxRepository = outboxRepository;
        this.idGenerator = idGenerator;
        this.shardRoutingOperations = ShardRoutingOperations.noop();
        this.ownershipGuard = OwnershipGuard.noop();
        this.productDetailCache = null;
    }

    @Autowired
    public ProductService(ProductRepository productRepository, OutboxRepository outboxRepository,
            SnowflakeIdGenerator idGenerator, ShardRoutingOperations shardRoutingOperations,
            OwnershipGuard ownershipGuard, ObjectProvider<TwoLevelCache<Long, ProductCacheEntry>> cacheProvider) {
        this.productRepository = productRepository;
        this.outboxRepository = outboxRepository;
        this.idGenerator = idGenerator;
        this.shardRoutingOperations = shardRoutingOperations;
        this.ownershipGuard = ownershipGuard;
        this.productDetailCache = cacheProvider.getIfAvailable();
    }

    @Caching(evict = @CacheEvict(value = "productSearch", allEntries = true))
    @Transactional
    public Product create(long spuId, String title, String category, BigDecimal price) {
        long skuId = idGenerator.nextId();
        return shardRoutingOperations.execute("product", skuId,
                () -> createInShard(skuId, spuId, title, category, price));
    }

    private Product createInShard(long skuId, long spuId, String title, String category, BigDecimal price) {
        ownershipGuard.checkWrite("product", skuId);
        Instant now = Instant.now();
        Product product = new Product(skuId, spuId, title, category, price, ProductStatus.DRAFT, now, now);
        Product saved = productRepository.save(product);
        cacheProduct(saved);
        appendProductChanged(saved);
        return saved;
    }

    public Product get(long skuId) {
        ProductCacheEntry entry = productDetailCache == null
                ? loadProductCacheEntry(skuId)
                : productDetailCache.get(skuId, () -> loadProductCacheEntry(skuId),
                        value -> value.present()
                                ? productTtlPolicy.ttlForKey(skuId)
                                : nullTtlPolicy.ttlForKey("product-miss:" + skuId));
        if (!entry.present()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "product not found");
        }
        return entry.product();
    }

    @Cacheable(value = "productSearch", key = "(#keyword == null ? '' : #keyword) + ':' + #limit")
    public List<Product> search(String keyword, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        return productRepository.search(keyword, safeLimit);
    }

    @Caching(evict = @CacheEvict(value = "productSearch", allEntries = true))
    @Transactional
    public Product changePrice(long skuId, BigDecimal price) {
        return shardRoutingOperations.execute("product", skuId, () -> {
            ownershipGuard.checkWrite("product", skuId);
            if (price.signum() <= 0) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "price must be positive");
            }
            Product saved = productRepository.save(get(skuId).changePrice(price));
            cacheProduct(saved);
            appendProductChanged(saved);
            return saved;
        });
    }

    @Caching(evict = @CacheEvict(value = "productSearch", allEntries = true))
    @Transactional
    public Product changeStatus(long skuId, ProductStatus status) {
        return shardRoutingOperations.execute("product", skuId, () -> {
            ownershipGuard.checkWrite("product", skuId);
            Product saved = productRepository.save(get(skuId).changeStatus(status));
            cacheProduct(saved);
            appendProductChanged(saved);
            return saved;
        });
    }

    @Caching(evict = @CacheEvict(value = "productSearch", allEntries = true))
    @Transactional
    public Product rename(long skuId, String title) {
        return shardRoutingOperations.execute("product", skuId, () -> {
            ownershipGuard.checkWrite("product", skuId);
            Product saved = productRepository.save(get(skuId).rename(title));
            cacheProduct(saved);
            appendProductChanged(saved);
            return saved;
        });
    }

    private ProductCacheEntry loadProductCacheEntry(long skuId) {
        return shardRoutingOperations.execute("product", skuId, () -> productRepository.findBySkuId(skuId)
                .map(ProductCacheEntry::hit).orElseGet(ProductCacheEntry::miss));
    }

    private void cacheProduct(Product product) {
        if (productDetailCache != null) {
            productDetailCache.put(product.skuId(), ProductCacheEntry.hit(product),
                    productTtlPolicy.ttlForKey(product.skuId()));
        }
    }

    private void appendProductChanged(Product product) {
        outboxRepository.save(OutboxEvent.create("product-event-" + idGenerator.nextId(), "Product",
                String.valueOf(product.skuId()), EventTypes.PRODUCT_CHANGED,
                Map.of("skuId", product.skuId(), "spuId", product.spuId(), "title", product.title(), "category",
                        product.category(), "price", product.price(), "status", product.status().name(), "saleable",
                        product.status() == ProductStatus.ON_SALE, "version", product.updatedAt().toEpochMilli(),
                        "updatedAt", product.updatedAt().toString())));
    }
}
