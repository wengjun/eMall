package com.emall.common.task;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

public class JdbcDistributedTaskLock implements DistributedTaskLock {
    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;
    private final String ownerId;

    public JdbcDistributedTaskLock(JdbcTemplate jdbcTemplate, Clock clock, String ownerId) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
        this.ownerId = ownerId;
    }

    @Override
    public boolean tryLock(String lockName, Duration ttl) {
        Instant now = clock.instant();
        Instant lockedUntil = now.plus(ttl);
        try {
            jdbcTemplate.update("""
                    insert into scheduled_task_lock(lock_name, owner_id, locked_until, updated_at)
                    values (?, ?, ?, ?)
                    """, lockName, ownerId, Timestamp.from(lockedUntil), Timestamp.from(now));
            return true;
        } catch (DuplicateKeyException ex) {
            return jdbcTemplate.update("""
                    update scheduled_task_lock
                    set owner_id = ?, locked_until = ?, updated_at = ?
                    where lock_name = ? and locked_until <= ?
                    """, ownerId, Timestamp.from(lockedUntil), Timestamp.from(now),
                    lockName, Timestamp.from(now)) == 1;
        }
    }

    @Override
    public void unlock(String lockName) {
        jdbcTemplate.update("""
                update scheduled_task_lock
                set locked_until = ?, updated_at = ?
                where lock_name = ? and owner_id = ?
                """, Timestamp.from(clock.instant()), Timestamp.from(clock.instant()), lockName, ownerId);
    }
}
