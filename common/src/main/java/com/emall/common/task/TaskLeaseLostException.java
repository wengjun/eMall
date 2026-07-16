package com.emall.common.task;

public class TaskLeaseLostException extends IllegalStateException {
    public TaskLeaseLostException(String lockName) {
        super("distributed task lease was lost: " + lockName);
    }
}
