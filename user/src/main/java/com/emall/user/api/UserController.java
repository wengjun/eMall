package com.emall.user.api;

import com.emall.common.api.ApiResponse;
import com.emall.user.domain.UserAccount;
import com.emall.user.domain.UserStatus;
import com.emall.user.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserProfileResponse> register(@Valid @RequestBody RegisterUserRequest request) {
        return ApiResponse.ok(toProfile(userService.register(request.mobile(), request.nickname())));
    }

    @GetMapping("/{userId}")
    public ApiResponse<UserProfileResponse> getUser(@PathVariable long userId) {
        return ApiResponse.ok(toProfile(userService.privacySnapshot(userId)));
    }

    @PatchMapping("/{userId}/nickname")
    public ApiResponse<UserProfileResponse> rename(@PathVariable long userId,
            @Valid @RequestBody RenameUserRequest request) {
        return ApiResponse.ok(toProfile(userService.rename(userId, request.nickname())));
    }

    @PatchMapping("/{userId}/status")
    public ApiResponse<UserProfileResponse> changeStatus(@PathVariable long userId,
            @Valid @RequestBody ChangeUserStatusRequest request) {
        return ApiResponse.ok(toProfile(userService.changeStatus(userId, request.status())));
    }

    @PostMapping("/{userId}/privacy-requests/apply")
    public ApiResponse<UserProfileResponse> applyPrivacyRequest(@PathVariable long userId,
            @Valid @RequestBody ApplyPrivacyRequest request) {
        return ApiResponse.ok(toProfile(userService.applyPrivacyRequest(userId, request.requestType())));
    }

    public record RegisterUserRequest(@NotBlank @Pattern(regexp = "^1[3-9]\\d{9}$") String mobile,
            @NotBlank @Size(max = 40) String nickname) {
    }

    public record RenameUserRequest(@NotBlank @Size(max = 40) String nickname) {
    }

    public record ChangeUserStatusRequest(@NotNull UserStatus status) {
    }

    public record ApplyPrivacyRequest(@NotBlank String requestType) {
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
