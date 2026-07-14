package com.emall.common.security;

import java.util.Optional;

public final class AuthenticationContext {
    private static final ThreadLocal<AuthenticatedPrincipal> CURRENT = new ThreadLocal<>();

    private AuthenticationContext() {
    }

    public static Optional<AuthenticatedPrincipal> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    static void set(AuthenticatedPrincipal principal) {
        CURRENT.set(principal);
    }

    static void clear() {
        CURRENT.remove();
    }
}
