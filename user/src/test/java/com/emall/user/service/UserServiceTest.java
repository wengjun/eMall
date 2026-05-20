package com.emall.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emall.common.exception.BusinessException;
import com.emall.common.id.SnowflakeIdGenerator;
import com.emall.user.domain.UserAccount;
import com.emall.user.domain.UserStatus;
import com.emall.user.repository.InMemoryUserRepository;
import org.junit.jupiter.api.Test;

class UserServiceTest {
    private final UserService userService = new UserService(new InMemoryUserRepository(), new SnowflakeIdGenerator(1));

    @Test
    void shouldRegisterAndRenameUser() {
        UserAccount created = userService.register("13800000000", "alice");
        UserAccount renamed = userService.rename(created.userId(), "alice-new");

        assertThat(created.mobile()).isEqualTo("13800000000");
        assertThat(created.nickname()).isEqualTo("alice");
        assertThat(created.status()).isEqualTo(UserStatus.NORMAL);
        assertThat(renamed.nickname()).isEqualTo("alice-new");
        assertThat(renamed.updatedAt()).isAfterOrEqualTo(created.updatedAt());
    }

    @Test
    void shouldRejectDuplicateMobile() {
        userService.register("13800000001", "alice");

        assertThatThrownBy(() -> userService.register("13800000001", "bob")).isInstanceOf(BusinessException.class)
                .hasMessageContaining("mobile already registered");
    }

    @Test
    void shouldRejectMissingUser() {
        assertThatThrownBy(() -> userService.get(404L)).isInstanceOf(BusinessException.class)
                .hasMessageContaining("user not found");
    }

    @Test
    void shouldRejectChangingClosedUser() {
        UserAccount created = userService.register("13800000002", "alice");

        UserAccount closed = userService.changeStatus(created.userId(), UserStatus.CLOSED);

        assertThat(closed.status()).isEqualTo(UserStatus.CLOSED);
        assertThatThrownBy(() -> userService.changeStatus(created.userId(), UserStatus.NORMAL))
                .isInstanceOf(BusinessException.class).hasMessageContaining("closed user cannot be changed");
    }

    @Test
    void shouldApplyPrivacyDeleteAndAllowMobileReuse() {
        UserAccount created = userService.register("13800000003", "alice");

        UserAccount erased = userService.applyPrivacyRequest(created.userId(), "delete");
        UserAccount reused = userService.register("13800000003", "bob");

        assertThat(erased.status()).isEqualTo(UserStatus.CLOSED);
        assertThat(erased.mobile()).startsWith("deleted-");
        assertThat(erased.nickname()).startsWith("deleted-user-");
        assertThat(reused.nickname()).isEqualTo("bob");
    }

    @Test
    void shouldApplyPrivacyFreezeAndReturnMaskedSnapshot() {
        UserAccount created = userService.register("13800000004", "alice");

        UserAccount frozen = userService.applyPrivacyRequest(created.userId(), "freeze");
        UserAccount snapshot = userService.privacySnapshot(created.userId());

        assertThat(frozen.status()).isEqualTo(UserStatus.FROZEN);
        assertThat(snapshot.mobile()).isEqualTo("138****0004");
    }
}
