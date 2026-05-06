package com.emall.user.service;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import com.emall.common.id.SnowflakeIdGenerator;
import com.emall.user.domain.UserAccount;
import com.emall.user.domain.UserStatus;
import com.emall.user.repository.UserRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final SnowflakeIdGenerator idGenerator;

    public UserService(UserRepository userRepository, SnowflakeIdGenerator idGenerator) {
        this.userRepository = userRepository;
        this.idGenerator = idGenerator;
    }

    @Transactional
    public synchronized UserAccount register(String mobile, String nickname) {
        userRepository.findByMobile(mobile).ifPresent(existing -> {
            throw new BusinessException(ErrorCode.CONFLICT, "mobile already registered");
        });
        Instant now = Instant.now();
        UserAccount user = new UserAccount(idGenerator.nextId(), mobile, nickname, UserStatus.NORMAL, now, now);
        return userRepository.save(user);
    }

    public UserAccount get(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "user not found"));
    }

    @Transactional
    public UserAccount rename(long userId, String nickname) {
        UserAccount user = get(userId);
        return userRepository.save(user.rename(nickname));
    }

    @Transactional
    public UserAccount changeStatus(long userId, UserStatus status) {
        UserAccount user = get(userId);
        if (user.status() == UserStatus.CLOSED) {
            throw new BusinessException(ErrorCode.CONFLICT, "closed user cannot be changed");
        }
        return userRepository.save(user.changeStatus(status));
    }
}
