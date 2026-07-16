package com.emall.loadtest;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;

final class SystemResourceSampler {
    private final com.sun.management.OperatingSystemMXBean operatingSystem;
    private final long startedNanos;
    private final long startedCpuNanos;
    private final long startedGcMillis;

    SystemResourceSampler() {
        this.operatingSystem =
                ManagementFactory.getOperatingSystemMXBean() instanceof com.sun.management.OperatingSystemMXBean bean
                        ? bean
                        : null;
        this.startedNanos = System.nanoTime();
        this.startedCpuNanos = operatingSystem == null ? 0L : operatingSystem.getProcessCpuTime();
        this.startedGcMillis = gcCollectionMillis();
    }

    Usage finish() {
        long wallNanos = Math.max(1L, System.nanoTime() - startedNanos);
        int processors = Runtime.getRuntime().availableProcessors();
        double cpu = operatingSystem == null
                ? -1.0
                : Math.max(0.0, Math.min(1.0,
                        (double) (operatingSystem.getProcessCpuTime() - startedCpuNanos) / wallNanos / processors));
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        ThreadMXBean threads = ManagementFactory.getThreadMXBean();
        return new Usage(cpu, memory.getHeapMemoryUsage().getUsed(), memory.getHeapMemoryUsage().getMax(),
                threads.getPeakThreadCount(), processors, Math.max(0L, gcCollectionMillis() - startedGcMillis));
    }

    private long gcCollectionMillis() {
        return ManagementFactory.getGarbageCollectorMXBeans().stream()
                .mapToLong(GarbageCollectorMXBean::getCollectionTime).filter(value -> value > 0L).sum();
    }

    record Usage(double processCpuUtilization, long heapUsedBytes, long heapMaxBytes, int peakThreads,
            int availableProcessors, long gcPauseMillis) {
    }
}
