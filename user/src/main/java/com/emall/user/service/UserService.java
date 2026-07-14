package com.emall.user.service;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import com.emall.common.id.SnowflakeIdGenerator;
import com.emall.common.region.OwnershipGuard;
import com.emall.common.sharding.ShardRoutingOperations;
import com.emall.common.sharding.ShardRouteIndex;
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
    private final ShardRouteIndex shardRouteIndex;

    public UserService(UserRepository userRepository, SnowflakeIdGenerator idGenerator) {
        this(userRepository, idGenerator, ShardRoutingOperations.noop(), OwnershipGuard.noop(),
                ShardRouteIndex.local());
    }

    @Autowired
    public UserService(UserRepository userRepository, SnowflakeIdGenerator idGenerator,
            ShardRoutingOperations shardRoutingOperations, OwnershipGuard ownershipGuard,
            ShardRouteIndex shardRouteIndex) {
        this.userRepository = userRepository;
        this.idGenerator = idGenerator;
        this.shardRoutingOperations = shardRoutingOperations;
        this.ownershipGuard = ownershipGuard;
        this.shardRouteIndex = shardRouteIndex;
    }

    @Transactional
    public UserAccount register(String mobile, String nickname) {
        return register(idGenerator.nextId(), mobile, nickname);
    }

    @Transactional
    public UserAccount register(long userId, String mobile, String nickname) {
        try {
            shardRouteIndex.bindUniqueTransactional("user-mobile", mobile, userId);
        } catch (BusinessException ex) {
            if (ex.errorCode() == ErrorCode.CONFLICT) {
                throw new BusinessException(ErrorCode.CONFLICT, "mobile already registered");
            }
            throw ex;
        }
        try {
            return registerInShard(userId, mobile, nickname);
        } catch (RuntimeException ex) {
            shardRouteIndex.removeIfOwned("user-mobile", mobile, userId);
            throw ex;
        }
    }

    private UserAccount registerInShard(long userId, String mobile, String nickname) {
        return shardRoutingOperations.execute("user_account", userId, () -> {
            userRepository.findByMobile(mobile).ifPresent(existing -> {
                throw new BusinessException(ErrorCode.CONFLICT, "mobile already registered");
            });
            Instant now = Instant.now();
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
                if (user.status() == UserStatus.CLOSED) {
                    return user;
                }
                UserAccount erased = userRepository.save(user.erasePersonalData());
                shardRouteIndex.removeIfOwnedTransactional("user-mobile", user.mobile(), userId);
                return erased;
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
