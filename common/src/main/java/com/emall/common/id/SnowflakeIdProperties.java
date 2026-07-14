package com.emall.common.id;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("emall.id")
public class SnowflakeIdProperties {
    private Long workerId;
    private boolean leaseEnabled;
    private Duration leaseTtl = Duration.ofSeconds(60);
    private Duration renewInterval = Duration.ofSeconds(15);
    private long maximumClockRollbackMillis = 5;

    public Long getWorkerId() {
        return workerId;
    }

    public void setWorkerId(Long workerId) {
        this.workerId = workerId;
    }

    public boolean isLeaseEnabled() {
        return leaseEnabled;
    }

    public void setLeaseEnabled(boolean leaseEnabled) {
        this.leaseEnabled = leaseEnabled;
    }

    public Duration getLeaseTtl() {
        return leaseTtl;
    }

    public void setLeaseTtl(Duration leaseTtl) {
        this.leaseTtl = leaseTtl;
    }

    public Duration getRenewInterval() {
        return renewInterval;
    }

    public void setRenewInterval(Duration renewInterval) {
        this.renewInterval = renewInterval;
    }

    public long getMaximumClockRollbackMillis() {
        return maximumClockRollbackMillis;
    }

    public void setMaximumClockRollbackMillis(long maximumClockRollbackMillis) {
        this.maximumClockRollbackMillis = maximumClockRollbackMillis;
    }
}
