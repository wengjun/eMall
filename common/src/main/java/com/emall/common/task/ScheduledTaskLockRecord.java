package com.emall.common.task;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@TableName("scheduled_task_lock")
@Getter
@Setter
public class ScheduledTaskLockRecord {
    @TableId(value = "lock_name", type = IdType.INPUT)
    private String lockName;

    @TableField("owner_id")
    private String ownerId;

    @TableField("locked_until")
    private LocalDateTime lockedUntil;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public ScheduledTaskLockRecord() {
    }

    public ScheduledTaskLockRecord(String lockName, String ownerId, LocalDateTime lockedUntil,
            LocalDateTime updatedAt) {
        this.lockName = lockName;
        this.ownerId = ownerId;
        this.lockedUntil = lockedUntil;
        this.updatedAt = updatedAt;
    }
}
