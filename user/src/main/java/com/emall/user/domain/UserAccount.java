package com.emall.user.domain;

import com.emall.common.privacy.SensitiveDataMasker;
import com.emall.common.privacy.SensitiveDataType;
import java.time.Instant;

public record UserAccount(long userId, String mobile, String nickname, UserStatus status, Instant createdAt,
        Instant updatedAt) {
    public UserAccount rename(String newNickname) {
        return new UserAccount(userId, mobile, newNickname, status, createdAt, Instant.now());
    }

    public UserAccount changeStatus(UserStatus newStatus) {
        return new UserAccount(userId, mobile, nickname, newStatus, createdAt, Instant.now());
    }

    public UserAccount erasePersonalData() {
        return new UserAccount(userId, "deleted-" + userId, "deleted-user-" + userId, UserStatus.CLOSED, createdAt,
                Instant.now());
    }

    public UserAccount maskSensitive() {
        return new UserAccount(userId, SensitiveDataMasker.mask(SensitiveDataType.MOBILE, mobile), nickname, status,
                createdAt, updatedAt);
    }
}
