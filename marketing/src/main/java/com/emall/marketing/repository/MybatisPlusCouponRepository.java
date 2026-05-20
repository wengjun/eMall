package com.emall.marketing.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.emall.marketing.domain.Coupon;
import com.emall.marketing.domain.CouponStatus;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
public class MybatisPlusCouponRepository implements CouponRepository {
    private final CouponMapper couponMapper;

    public MybatisPlusCouponRepository(CouponMapper couponMapper) {
        this.couponMapper = couponMapper;
    }

    @Override
    public Coupon save(Coupon coupon) {
        CouponEntity entity = toEntity(coupon);
        try {
            couponMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            couponMapper.update(null,
                    new UpdateWrapper<CouponEntity>().set("status", entity.getStatus())
                            .set("expires_at", entity.getExpiresAt()).set("updated_at", entity.getUpdatedAt())
                            .eq("coupon_id", entity.getCouponId()));
        }
        return coupon;
    }

    @Override
    public Optional<Coupon> findById(String couponId) {
        return Optional.ofNullable(couponMapper.selectById(couponId)).map(this::toDomain);
    }

    @Override
    public List<Coupon> findByUserId(long userId) {
        return couponMapper.selectList(new QueryWrapper<CouponEntity>().eq("user_id", userId).orderByDesc("updated_at"))
                .stream().map(this::toDomain).toList();
    }

    private CouponEntity toEntity(Coupon coupon) {
        CouponEntity entity = new CouponEntity();
        entity.setCouponId(coupon.couponId());
        entity.setUserId(coupon.userId());
        entity.setThresholdAmount(coupon.thresholdAmount());
        entity.setDiscountAmount(coupon.discountAmount());
        entity.setStatus(coupon.status().name());
        entity.setExpiresAt(LocalDateTime.ofInstant(coupon.expiresAt(), ZoneOffset.UTC));
        entity.setUpdatedAt(LocalDateTime.ofInstant(coupon.updatedAt(), ZoneOffset.UTC));
        return entity;
    }

    private Coupon toDomain(CouponEntity entity) {
        return new Coupon(entity.getCouponId(), entity.getUserId(), entity.getThresholdAmount(),
                entity.getDiscountAmount(), CouponStatus.valueOf(entity.getStatus()),
                entity.getExpiresAt().toInstant(ZoneOffset.UTC), entity.getUpdatedAt().toInstant(ZoneOffset.UTC));
    }
}
