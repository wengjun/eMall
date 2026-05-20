package com.emall.pricing.repository;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.emall.pricing.domain.PriceBook;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
public class MybatisPlusPriceRepository implements PriceRepository {
    private final PriceBookMapper priceBookMapper;

    public MybatisPlusPriceRepository(PriceBookMapper priceBookMapper) {
        this.priceBookMapper = priceBookMapper;
    }

    @Override
    public PriceBook save(PriceBook priceBook) {
        PriceBookEntity entity = toEntity(priceBook);
        try {
            priceBookMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            priceBookMapper.update(null,
                    new UpdateWrapper<PriceBookEntity>().set("list_price", entity.getListPrice())
                            .set("sale_price", entity.getSalePrice()).set("currency", entity.getCurrency())
                            .set("version", entity.getVersion()).set("active", entity.getActive())
                            .set("updated_at", entity.getUpdatedAt()).eq("sku_id", entity.getSkuId()));
        }
        return priceBook;
    }

    @Override
    public Optional<PriceBook> findBySkuId(long skuId) {
        return Optional.ofNullable(priceBookMapper.selectById(skuId)).map(this::toDomain);
    }

    private PriceBookEntity toEntity(PriceBook priceBook) {
        PriceBookEntity entity = new PriceBookEntity();
        entity.setSkuId(priceBook.skuId());
        entity.setListPrice(priceBook.listPrice());
        entity.setSalePrice(priceBook.salePrice());
        entity.setCurrency(priceBook.currency());
        entity.setVersion(priceBook.version());
        entity.setActive(priceBook.active());
        entity.setUpdatedAt(LocalDateTime.ofInstant(priceBook.updatedAt(), ZoneOffset.UTC));
        return entity;
    }

    private PriceBook toDomain(PriceBookEntity entity) {
        return new PriceBook(entity.getSkuId(), entity.getListPrice(), entity.getSalePrice(), entity.getCurrency(),
                entity.getVersion(), entity.getActive(), entity.getUpdatedAt().toInstant(ZoneOffset.UTC));
    }
}
