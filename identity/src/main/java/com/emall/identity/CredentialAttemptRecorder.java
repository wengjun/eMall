package com.emall.identity;

import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
class CredentialAttemptRecorder {
    static final int MAXIMUM_ATTEMPTS = 5;
    static final Duration LOCK_DURATION = Duration.ofMinutes(15);
    private final IdentityRepository repository;

    CredentialAttemptRecorder(IdentityRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void recordFailure(IdentityCredential credential, Instant now) {
        repository.recordCredentialFailure(credential.accountId(), now, now.plus(LOCK_DURATION), MAXIMUM_ATTEMPTS);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void recordSuccess(IdentityCredential credential, Instant now) {
        if (credential.failedAttempts() > 0 || credential.lockedUntil() != null) {
            repository.clearCredentialFailures(credential.accountId(), now);
        }
    }
}
