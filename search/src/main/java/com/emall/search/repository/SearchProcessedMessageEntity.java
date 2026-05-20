package com.emall.search.repository;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@TableName("processed_message")
@Getter
@Setter
public class SearchProcessedMessageEntity {
    @TableId(value = "message_id", type = IdType.INPUT)
    private String messageId;

    @TableField("processed_at")
    private LocalDateTime processedAt;

    @TableField("status")
    private String status;

    @TableField("retry_count")
    private Integer retryCount;

    @TableField("last_error_code")
    private String lastErrorCode;

    @TableField("last_error")
    private String lastError;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField("dead_at")
    private LocalDateTime deadAt;
}
