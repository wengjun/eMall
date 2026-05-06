create table scheduled_task_lock (
    lock_name varchar(128) primary key,
    owner_id varchar(256) not null,
    locked_until timestamp(6) not null,
    updated_at timestamp(6) not null,
    index idx_scheduled_task_lock_until (locked_until)
);
