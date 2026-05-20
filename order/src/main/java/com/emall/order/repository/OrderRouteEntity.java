package com.emall.order.repository;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@TableName("order_route_index")
@Getter
@Setter
public class OrderRouteEntity {
    @TableId(value = "order_id", type = IdType.INPUT)
    private Long orderId;

    @TableField("request_id")
    private String requestId;

    @TableField("user_id")
    private Long userId;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
