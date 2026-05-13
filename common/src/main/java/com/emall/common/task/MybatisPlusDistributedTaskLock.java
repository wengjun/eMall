package com.emall.common.task;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.dao.DuplicateKeyException;

public class MybatisPlusDistributedTaskLock implements DistributedTaskLock {
    private final ScheduledTaskLockMapper taskLockMapper;
    private final Clock clock;
    private final String ownerId;

    public MybatisPlusDistributedTaskLock(ScheduledTaskLockMapper taskLockMapper, Clock clock, String ownerId) {
        this.taskLockMapper = taskLockMapper;
        this.clock = clock;
        this.ownerId = ownerId;
    }

    @Override
    public boolean tryLock(String lockName, Duration ttl) {
        Instant nowInstant = clock.instant();
        LocalDateTime now = databaseTime(nowInstant);
        LocalDateTime lockedUntil = databaseTime(nowInstant.plus(ttl));
        try {
            taskLockMapper.insert(new ScheduledTaskLockRecord(lockName, ownerId, lockedUntil, now));
            return true;
        } catch (DuplicateKeyException ex) {
            return taskLockMapper.update(null, new UpdateWrapper<ScheduledTaskLockRecord>()
                    .set("owner_id", ownerId)
                    .set("locked_until", lockedUntil)
                    .set("updated_at", now)
                    .eq("lock_name", lockName)
                    .le("locked_until", now)) == 1;
        }
    }

    @Override
    public void unlock(String lockName) {
        LocalDateTime now = databaseTime(clock.instant());
        taskLockMapper.update(null, new UpdateWrapper<ScheduledTaskLockRecord>()
                .set("locked_until", now)
                .set("updated_at", now)
                .eq("lock_name", lockName)
                .eq("owner_id", ownerId));
    }

    private LocalDateTime databaseTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
