package com.emall.common.controlplane;

public enum ControlPlaneOperationStatus {
    PENDING,
    APPLYING,
    VERIFYING,
    RETRYING,
    SUCCEEDED,
    ROLLING_BACK,
    ROLLED_BACK,
    FAILED;

    public boolean terminal() {
        return this == SUCCEEDED || this == ROLLED_BACK || this == FAILED;
    }
}
