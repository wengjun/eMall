package com.emall.identity;

record AccountRegistration(String registrationId, long accountId, IdentityStatus accountStatus,
        ProfileProjectionStatus profileStatus) {
}
