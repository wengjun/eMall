package com.emall.recommendation.repository;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@TableName("recommendation_experiment")
@Getter
@Setter
public class RecommendationExperimentEntity {
    @TableId(value = "experiment_id", type = IdType.INPUT)
    private Long experimentId;

    @TableField("scene")
    private String scene;

    @TableField("name")
    private String name;

    @TableField("traffic_percent")
    private Integer trafficPercent;

    @TableField("control_strategy")
    private String controlStrategy;

    @TableField("treatment_strategy")
    private String treatmentStrategy;

    @TableField("status")
    private String status;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
