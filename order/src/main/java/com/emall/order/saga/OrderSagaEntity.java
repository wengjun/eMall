package com.emall.order.saga;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@TableName("order_create_saga")
@Getter
@Setter
public class OrderSagaEntity {
    @TableId(value = "saga_id", type = IdType.INPUT)
    private Long sagaId;
    private String requestId;
    private Long orderId;
    private Long userId;
    private Long skuId;
    private String couponId;
    private String inventoryReservationId;
    private String stage;
    private String status;
    private Integer attempts;
    private String lastError;
    private LocalDateTime nextRetryAt;
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
