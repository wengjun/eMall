package com.emall.recommendation.repository;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("recommendation_experiment")
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

    public Long getExperimentId() {
        return experimentId;
    }

    public void setExperimentId(Long experimentId) {
        this.experimentId = experimentId;
    }

    public String getScene() {
        return scene;
    }

    public void setScene(String scene) {
        this.scene = scene;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getTrafficPercent() {
        return trafficPercent;
    }

    public void setTrafficPercent(Integer trafficPercent) {
        this.trafficPercent = trafficPercent;
    }

    public String getControlStrategy() {
        return controlStrategy;
    }

    public void setControlStrategy(String controlStrategy) {
        this.controlStrategy = controlStrategy;
    }

    public String getTreatmentStrategy() {
        return treatmentStrategy;
    }

    public void setTreatmentStrategy(String treatmentStrategy) {
        this.treatmentStrategy = treatmentStrategy;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
