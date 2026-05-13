package com.emall.user.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.emall.common.crypto.FieldEncryptor;
import com.emall.user.domain.UserAccount;
import com.emall.user.domain.UserStatus;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
public class MybatisPlusUserRepository implements UserRepository {
    private final UserAccountMapper userAccountMapper;
    private final FieldEncryptor fieldEncryptor;

    public MybatisPlusUserRepository(UserAccountMapper userAccountMapper, FieldEncryptor fieldEncryptor) {
        this.userAccountMapper = userAccountMapper;
        this.fieldEncryptor = fieldEncryptor;
    }

    @Override
    public UserAccount save(UserAccount user) {
        UserAccountEntity entity = toEntity(user);
        try {
            userAccountMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            userAccountMapper.update(null, new UpdateWrapper<UserAccountEntity>()
                    .set("mobile", entity.getMobile())
                    .set("mobile_ciphertext", entity.getMobileCiphertext())
                    .set("mobile_hash", entity.getMobileHash())
                    .set("nickname", entity.getNickname())
                    .set("status", entity.getStatus())
                    .set("updated_at", entity.getUpdatedAt())
                    .eq("user_id", entity.getUserId()));
        }
        return user;
    }

    @Override
    public Optional<UserAccount> findById(long userId) {
        return Optional.ofNullable(userAccountMapper.selectById(userId)).map(this::toDomain);
    }

    @Override
    public Optional<UserAccount> findByMobile(String mobile) {
        String mobileHash = fieldEncryptor.lookupHash(mobile);
        return Optional.ofNullable(userAccountMapper.selectOne(new QueryWrapper<UserAccountEntity>()
                .eq("mobile_hash", mobileHash)
                .or()
                .eq("mobile", mobile))).map(this::toDomain);
    }

    private UserAccountEntity toEntity(UserAccount user) {
        String mobileHash = fieldEncryptor.lookupHash(user.mobile());
        UserAccountEntity entity = new UserAccountEntity();
        entity.setUserId(user.userId());
        entity.setMobile(mobileHash);
        entity.setMobileCiphertext(fieldEncryptor.encrypt(user.mobile()));
        entity.setMobileHash(mobileHash);
        entity.setNickname(user.nickname());
        entity.setStatus(user.status().name());
        entity.setCreatedAt(LocalDateTime.ofInstant(user.createdAt(), ZoneOffset.UTC));
        entity.setUpdatedAt(LocalDateTime.ofInstant(user.updatedAt(), ZoneOffset.UTC));
        return entity;
    }

    private UserAccount toDomain(UserAccountEntity entity) {
        String encryptedMobile = entity.getMobileCiphertext();
        String mobile = encryptedMobile == null || encryptedMobile.isBlank()
                ? entity.getMobile()
                : fieldEncryptor.decrypt(encryptedMobile);
        return new UserAccount(entity.getUserId(), mobile, entity.getNickname(),
                UserStatus.valueOf(entity.getStatus()), entity.getCreatedAt().toInstant(ZoneOffset.UTC),
                entity.getUpdatedAt().toInstant(ZoneOffset.UTC));
    }
}
