package com.emall.user.service;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import com.emall.common.id.SnowflakeIdGenerator;
import com.emall.common.region.OwnershipGuard;
import com.emall.common.sharding.ShardRoutingOperations;
import com.emall.user.domain.UserAccount;
import com.emall.user.domain.UserStatus;
import com.emall.user.repository.UserRepository;
import java.time.Instant;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final SnowflakeIdGenerator idGenerator;
    private final ShardRoutingOperations shardRoutingOperations;
    private final OwnershipGuard ownershipGuard;

    public UserService(UserRepository userRepository, SnowflakeIdGenerator idGenerator) {
        this(userRepository, idGenerator, ShardRoutingOperations.noop(), OwnershipGuard.noop());
    }

    @Autowired
    public UserService(UserRepository userRepository, SnowflakeIdGenerator idGenerator,
            ShardRoutingOperations shardRoutingOperations, OwnershipGuard ownershipGuard) {
        this.userRepository = userRepository;
        this.idGenerator = idGenerator;
        this.shardRoutingOperations = shardRoutingOperations;
        this.ownershipGuard = ownershipGuard;
    }

    @Transactional
    public UserAccount register(String mobile, String nickname) {
        return shardRoutingOperations.execute("user_account", mobile, () -> {
            userRepository.findByMobile(mobile).ifPresent(existing -> {
                throw new BusinessException(ErrorCode.CONFLICT, "mobile already registered");
            });
            Instant now = Instant.now();
            long userId = idGenerator.nextId();
            ownershipGuard.checkWrite("user", userId);
            UserAccount user = new UserAccount(userId, mobile, nickname, UserStatus.NORMAL, now, now);
            return userRepository.save(user);
        });
    }

    public UserAccount get(long userId) {
        return shardRoutingOperations.execute("user_account", userId, () -> userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "user not found")));
    }

    @Transactional
    public UserAccount rename(long userId, String nickname) {
        return shardRoutingOperations.execute("user_account", userId, () -> {
            ownershipGuard.checkWrite("user", userId);
            UserAccount user = get(userId);
            return userRepository.save(user.rename(nickname));
        });
    }

    @Transactional
    public UserAccount changeStatus(long userId, UserStatus status) {
        return shardRoutingOperations.execute("user_account", userId, () -> {
            ownershipGuard.checkWrite("user", userId);
            UserAccount user = get(userId);
            if (user.status() == UserStatus.CLOSED) {
                throw new BusinessException(ErrorCode.CONFLICT, "closed user cannot be changed");
            }
            return userRepository.save(user.changeStatus(status));
        });
    }

    @Transactional
    public UserAccount applyPrivacyRequest(long userId, String requestType) {
        return shardRoutingOperations.execute("user_account", userId, () -> {
            ownershipGuard.checkWrite("user", userId);
            UserAccount user = get(userId);
            String normalizedType = normalizeRequestType(requestType);
            if ("freeze".equals(normalizedType)) {
                return userRepository.save(user.changeStatus(UserStatus.FROZEN));
            }
            if ("delete".equals(normalizedType) || "erase".equals(normalizedType)) {
                return user.status() == UserStatus.CLOSED ? user : userRepository.save(user.erasePersonalData());
            }
            throw new BusinessException(ErrorCode.BAD_REQUEST, "unsupported privacy request type");
        });
    }

    public UserAccount privacySnapshot(long userId) {
        return get(userId).maskSensitive();
    }

    private String normalizeRequestType(String requestType) {
        String normalized = requestType == null ? "" : requestType.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "privacy request type must not be blank");
        }
        return normalized;
    }
}
