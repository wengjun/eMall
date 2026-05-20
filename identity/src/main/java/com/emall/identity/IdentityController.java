package com.emall.identity;

import com.emall.common.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/identity")
class IdentityController {
    private final IdentityService identityService;

    IdentityController(IdentityService identityService) {
        this.identityService = identityService;
    }

    @PostMapping("/accounts")
    ApiResponse<IdentityAccount> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        return ApiResponse.ok(identityService.createAccount(request.type(), request.subject(), request.displayName()));
    }

    @PostMapping("/sessions")
    ApiResponse<AuthToken> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(identityService.login(request.subject(), request.deviceId()));
    }

    @PatchMapping("/sessions/{sessionId}/revoke")
    ApiResponse<DeviceSession> revokeSession(@PathVariable long sessionId) {
        return ApiResponse.ok(identityService.revokeSession(sessionId));
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
    ApiResponse<ServiceClient> registerServiceClient(@Valid @RequestBody RegisterServiceClientRequest request) {
        return ApiResponse.ok(
                identityService.registerServiceClient(request.clientKey(), request.clientSecret(), request.scopes()));
    }

    @PostMapping("/merchants/{merchantId}/sub-accounts")
    ApiResponse<MerchantSubAccount> createMerchantSubAccount(@PathVariable long merchantId,
            @Valid @RequestBody CreateMerchantSubAccountRequest request) {
        return ApiResponse
                .ok(identityService.createMerchantSubAccount(merchantId, request.accountId(), request.roleCode()));
    }

    record CreateAccountRequest(@NotNull IdentityType type, @NotBlank String subject, @NotBlank String displayName) {
    }

    record LoginRequest(@NotBlank String subject, @NotBlank String deviceId) {
    }

    record ValidateSessionRequest(@NotBlank String accessToken, @NotBlank String scope, @NotBlank String resource) {
    }

    record GrantPermissionRequest(@NotBlank String scope, @NotBlank String resource) {
    }

    record RegisterServiceClientRequest(@NotBlank String clientKey, @NotBlank String clientSecret,
            @NotBlank String scopes) {
    }

    record CreateMerchantSubAccountRequest(@Positive long accountId, @NotBlank String roleCode) {
    }
}
