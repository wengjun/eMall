package com.emall.identity;

import com.emall.common.api.ApiResponse;
import com.emall.common.security.AuthorizationGuard;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;

@RestController
@RequestMapping("/api/identity")
class IdentityController {
    private final IdentityService identityService;
    private final AccountLifecycleService lifecycleService;
    private final AuthorizationGuard authorizationGuard;

    IdentityController(IdentityService identityService, AccountLifecycleService lifecycleService,
            AuthorizationGuard authorizationGuard) {
        this.identityService = identityService;
        this.lifecycleService = lifecycleService;
        this.authorizationGuard = authorizationGuard;
    }

    @PostMapping("/registrations")
    @ResponseStatus(HttpStatus.ACCEPTED)
    ApiResponse<AccountRegistration> register(@RequestHeader("Idempotency-Key") String registrationId,
            @Valid @RequestBody RegisterAccountRequest request) {
        return ApiResponse.ok(
                lifecycleService.register(registrationId, request.mobile(), request.displayName(), request.password()));
    }

    @GetMapping("/registrations/{registrationId}")
    ApiResponse<AccountRegistration> registrationStatus(@PathVariable String registrationId) {
        return ApiResponse.ok(lifecycleService.registrationStatus(registrationId));
    }

    @PatchMapping("/accounts/{accountId}/lifecycle")
    ApiResponse<IdentityAccount> changeAccountLifecycle(@PathVariable long accountId,
            @Valid @RequestBody ChangeAccountLifecycleRequest request) {
        authorizationGuard.requireOwnerOrOperator(accountId);
        return ApiResponse.ok(lifecycleService.changeStatus(accountId, request.action(), request.reason()));
    }

    @PostMapping("/sessions")
    ApiResponse<AuthToken> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(identityService.login(request.subject(), request.password(), request.deviceId()));
    }

    @PostMapping("/sessions/refresh")
    ApiResponse<AuthToken> refresh(@Valid @RequestBody RefreshSessionRequest request) {
        return ApiResponse.ok(identityService.refresh(request.refreshToken(), request.deviceId()));
    }

    @PostMapping("/service-sessions")
    ApiResponse<AuthToken> authenticateServiceClient(@Valid @RequestBody ServiceSessionRequest request) {
        return ApiResponse.ok(identityService.authenticateServiceClient(request.clientKey(), request.clientSecret()));
    }

    @PatchMapping("/sessions/{sessionId}/revoke")
    ApiResponse<SessionView> revokeSession(@PathVariable long sessionId) {
        DeviceSession session = identityService.revokeSession(sessionId);
        return ApiResponse.ok(new SessionView(session.sessionId(), session.accountId(), session.deviceId(),
                session.status(), session.expiresAt(), session.updatedAt()));
    }

    @PostMapping("/sessions/validate")
    ApiResponse<SessionValidation> validateSession(@Valid @RequestBody ValidateSessionRequest request) {
        return ApiResponse
                .ok(identityService.validateSession(request.accessToken(), request.scope(), request.resource()));
    }

    @PostMapping("/accounts/{accountId}/permissions")
    ApiResponse<PermissionGrant> grantPermission(@PathVariable long accountId,
            @Valid @RequestBody GrantPermissionRequest request) {
        return ApiResponse.ok(identityService.grantPermission(accountId, request.scope(), request.resource()));
    }

    @GetMapping("/access")
    ApiResponse<AccessDecision> checkAccess(@RequestParam long accountId, @RequestParam String scope,
            @RequestParam String resource) {
        return ApiResponse.ok(identityService.checkAccess(accountId, scope, resource));
    }

    @PostMapping("/service-clients")
    ApiResponse<ServiceClientView> registerServiceClient(@Valid @RequestBody RegisterServiceClientRequest request) {
        ServiceClient client =
                identityService.registerServiceClient(request.clientKey(), request.clientSecret(), request.scopes());
        return ApiResponse.ok(new ServiceClientView(client.clientId(), client.clientKey(), client.scopes(),
                client.active(), client.createdAt(), client.updatedAt()));
    }

    @PostMapping("/merchants/{merchantId}/sub-accounts")
    ApiResponse<MerchantSubAccount> createMerchantSubAccount(@PathVariable long merchantId,
            @Valid @RequestBody CreateMerchantSubAccountRequest request) {
        return ApiResponse
                .ok(identityService.createMerchantSubAccount(merchantId, request.accountId(), request.roleCode()));
    }

    record RegisterAccountRequest(@NotBlank @Pattern(regexp = "^1[3-9]\\d{9}$") String mobile,
            @NotBlank @Size(max = 128) String displayName, @NotBlank @Size(min = 12, max = 128) String password) {
    }

    record ChangeAccountLifecycleRequest(@jakarta.validation.constraints.NotNull AccountLifecycleAction action,
            @NotBlank @Size(max = 256) String reason) {
    }

    record LoginRequest(@NotBlank String subject, @NotBlank String password, @NotBlank String deviceId) {
    }

    record RefreshSessionRequest(@NotBlank String refreshToken, @NotBlank String deviceId) {
    }

    record ServiceSessionRequest(@NotBlank String clientKey, @NotBlank String clientSecret) {
    }

    record ValidateSessionRequest(@NotBlank String accessToken, @NotBlank String scope, @NotBlank String resource) {
    }

    record GrantPermissionRequest(@NotBlank String scope, @NotBlank String resource) {
    }

    record RegisterServiceClientRequest(@NotBlank String clientKey,
            @NotBlank @Size(min = 12, max = 128) String clientSecret, @NotBlank String scopes) {
    }

    record CreateMerchantSubAccountRequest(@Positive long accountId, @NotBlank String roleCode) {
    }

    record SessionView(long sessionId, long accountId, String deviceId, SessionStatus status, Instant expiresAt,
            Instant updatedAt) {
    }

    record ServiceClientView(long clientId, String clientKey, String scopes, boolean active, Instant createdAt,
            Instant updatedAt) {
    }
}
