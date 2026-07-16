package com.emall.loadtest;

record RequestResult(boolean success, long latencyMicros, int statusCode, String errorKind) {
    static RequestResult failed(long latencyMicros, String errorKind) {
        return new RequestResult(false, latencyMicros, 0, errorKind);
    }
}
