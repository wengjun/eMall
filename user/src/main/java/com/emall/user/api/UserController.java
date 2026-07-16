package com.emall.user.api;

import com.emall.common.api.ApiResponse;
import com.emall.common.security.AuthorizationGuard;
import com.emall.user.domain.UserAccount;
import com.emall.user.domain.UserStatus;
import com.emall.user.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    private final AuthorizationGuard authorizationGuard;

    public UserController(UserService userService) {
        this(userService, AuthorizationGuard.noop());
    }

    @Autowired
    public UserController(UserService userService, AuthorizationGuard authorizationGuard) {
        this.userService = userService;
        this.authorizationGuard = authorizationGuard;
    }

    @GetMapping("/{userId}")
    public ApiResponse<UserProfileResponse> getUser(@PathVariable long userId) {
        authorizationGuard.requireOwnerOrOperator(userId);
        return ApiResponse.ok(toProfile(userService.privacySnapshot(userId)));
    }

    @PatchMapping("/{userId}/nickname")
    public ApiResponse<UserProfileResponse> rename(@PathVariable long userId,
            @Valid @RequestBody RenameUserRequest request) {
        authorizationGuard.requireOwnerOrOperator(userId);
        return ApiResponse.ok(toProfile(userService.rename(userId, request.nickname())));
    }

    public record RenameUserRequest(@NotBlank @Size(max = 40) String nickname) {
    }

    public record UserProfileResponse(long userId, String mobile, String nickname, UserStatus status, Instant createdAt,
            Instant updatedAt) {
    }

    private UserProfileResponse toProfile(UserAccount user) {
        UserAccount masked = user.maskSensitive();
        return new UserProfileResponse(masked.userId(), masked.mobile(), masked.nickname(), masked.status(),
                masked.createdAt(), masked.updatedAt());
    }
}
