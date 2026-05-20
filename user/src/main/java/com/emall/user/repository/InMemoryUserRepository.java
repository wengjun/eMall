package com.emall.user.repository;

import com.emall.user.domain.UserAccount;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "memory")
public class InMemoryUserRepository implements UserRepository {
    private final ConcurrentMap<Long, UserAccount> byId = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Long> idByMobile = new ConcurrentHashMap<>();

    @Override
    public UserAccount save(UserAccount user) {
        UserAccount previous = byId.put(user.userId(), user);
        if (previous != null && !previous.mobile().equals(user.mobile())) {
            idByMobile.remove(previous.mobile(), user.userId());
        }
        idByMobile.put(user.mobile(), user.userId());
        return user;
    }

    @Override
    public Optional<UserAccount> findById(long userId) {
        return Optional.ofNullable(byId.get(userId));
    }

    @Override
    public Optional<UserAccount> findByMobile(String mobile) {
        Long userId = idByMobile.get(mobile);
        return userId == null ? Optional.empty() : findById(userId);
    }
}
