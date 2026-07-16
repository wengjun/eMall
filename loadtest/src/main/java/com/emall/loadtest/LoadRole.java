package com.emall.loadtest;

enum LoadRole {
    STANDALONE,
    WORKER,
    COORDINATOR;

    static LoadRole from(String value) {
        return valueOf(value.trim().replace('-', '_').toUpperCase());
    }
}
