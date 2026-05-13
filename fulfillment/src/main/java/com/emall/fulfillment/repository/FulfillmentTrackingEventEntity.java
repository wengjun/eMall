package com.emall.fulfillment.repository;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@TableName("fulfillment_tracking_event")
@Getter
@Setter
public class FulfillmentTrackingEventEntity {
    @TableId(value = "event_id", type = IdType.INPUT)
    private Long eventId;

    @TableField("fulfillment_id")
    private Long fulfillmentId;

    @TableField("carrier_code")
    private String carrierCode;

    @TableField("tracking_no")
    private String trackingNo;

    @TableField("event_code")
    private String eventCode;

    @TableField("event_time")
    private LocalDateTime eventTime;

    @TableField("location")
    private String location;

    @TableField("description")
    private String description;

    @TableField("received_at")
    private LocalDateTime receivedAt;
}
