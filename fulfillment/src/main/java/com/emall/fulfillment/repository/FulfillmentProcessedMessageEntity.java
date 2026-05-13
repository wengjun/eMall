package com.emall.fulfillment.repository;

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
public class FulfillmentProcessedMessageEntity {
    @TableId(value = "message_id", type = IdType.INPUT)
    private String messageId;

    @TableField("processed_at")
    private LocalDateTime processedAt;
}
