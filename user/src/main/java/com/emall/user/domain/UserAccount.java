package com.emall.user.domain;

import java.time.Instant;

public record UserAccount(long userId, String mobile, String nickname, UserStatus status, Instant createdAt,
        Instant updatedAt) {
    public UserAccount rename(String newNickname) {
        return new UserAccount(userId, mobile, newNickname, status, createdAt, Instant.now());
    }

    public UserAccount changeStatus(UserStatus newStatus) {
        return new UserAccount(userId, mobile, nickname, newStatus, createdAt, Instant.now());
    }
}
