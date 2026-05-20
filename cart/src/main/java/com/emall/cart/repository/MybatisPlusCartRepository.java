package com.emall.cart.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.emall.cart.domain.CartItem;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
public class MybatisPlusCartRepository implements CartRepository {
    private final CartItemMapper cartItemMapper;

    public MybatisPlusCartRepository(CartItemMapper cartItemMapper) {
        this.cartItemMapper = cartItemMapper;
    }

    @Override
    public CartItem save(CartItem item) {
        CartItemEntity entity = toEntity(item);
        try {
            cartItemMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            cartItemMapper.update(null,
                    new UpdateWrapper<CartItemEntity>().set("quantity", entity.getQuantity())
                            .set("selected", entity.getSelected()).set("updated_at", entity.getUpdatedAt())
                            .eq("user_id", entity.getUserId()).eq("sku_id", entity.getSkuId()));
        }
        return item;
    }

    @Override
    public Optional<CartItem> addQuantity(CartItem item, int maxQuantity) {
        CartItemEntity entity = toEntity(item);
        try {
            cartItemMapper.insert(entity);
            return Optional.of(item);
        } catch (DuplicateKeyException ex) {
            int updated = cartItemMapper.update(null,
                    new UpdateWrapper<CartItemEntity>().setSql("quantity = quantity + {0}", item.quantity())
                            .set("selected", entity.getSelected()).set("updated_at", entity.getUpdatedAt())
                            .eq("user_id", entity.getUserId()).eq("sku_id", entity.getSkuId())
                            .le("quantity", maxQuantity - item.quantity()));
            return updated == 1 ? find(item.userId(), item.skuId()) : Optional.empty();
        }
    }

    @Override
    public Optional<CartItem> find(long userId, long skuId) {
        return Optional
                .ofNullable(cartItemMapper
                        .selectOne(new QueryWrapper<CartItemEntity>().eq("user_id", userId).eq("sku_id", skuId)))
                .map(this::toDomain);
    }

    @Override
    public List<CartItem> findByUserId(long userId) {
        return cartItemMapper
                .selectList(new QueryWrapper<CartItemEntity>().eq("user_id", userId).orderByDesc("updated_at")).stream()
                .map(this::toDomain).toList();
    }

    @Override
    public void delete(long userId, long skuId) {
        cartItemMapper.delete(new QueryWrapper<CartItemEntity>().eq("user_id", userId).eq("sku_id", skuId));
    }

    @Override
    public void clearSelected(long userId) {
        cartItemMapper.delete(new QueryWrapper<CartItemEntity>().eq("user_id", userId).eq("selected", true));
    }

    private CartItemEntity toEntity(CartItem item) {
        CartItemEntity entity = new CartItemEntity();
        entity.setUserId(item.userId());
        entity.setSkuId(item.skuId());
        entity.setQuantity(item.quantity());
        entity.setSelected(item.selected());
        entity.setUpdatedAt(LocalDateTime.ofInstant(item.updatedAt(), ZoneOffset.UTC));
        return entity;
    }

    private CartItem toDomain(CartItemEntity entity) {
        return new CartItem(entity.getUserId(), entity.getSkuId(), entity.getQuantity(), entity.getSelected(),
                entity.getUpdatedAt().toInstant(ZoneOffset.UTC));
    }
}
