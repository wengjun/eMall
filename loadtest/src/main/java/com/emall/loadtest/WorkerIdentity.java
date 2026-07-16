package com.emall.loadtest;

record WorkerIdentity(int index, int count) {
    WorkerIdentity {
        if (count <= 0) {
            throw new IllegalArgumentException("worker count must be positive");
        }
        if (index < 0 || index >= count) {
            throw new IllegalArgumentException("worker index must be in [0, worker count)");
        }
    }

    int localRate(int globalRate) {
        int baseRate = globalRate / count;
        return baseRate + (index < globalRate % count ? 1 : 0);
    }

    long globalSequence(long localSequence) {
        if (localSequence <= 0) {
            throw new IllegalArgumentException("local sequence must be positive");
        }
        return Math.addExact(Math.multiplyExact(localSequence - 1L, count), index + 1L);
    }
}
