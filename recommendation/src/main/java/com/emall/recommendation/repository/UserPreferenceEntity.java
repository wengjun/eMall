package com.emall.recommendation.repository;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@TableName("recommendation_user_preference")
@Getter
@Setter
public class UserPreferenceEntity {
    @TableId(value = "user_id", type = IdType.INPUT)
    private Long userId;

    @TableField("category_code")
    private String categoryCode;

    @TableField("affinity_score")
    private Integer affinityScore;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
