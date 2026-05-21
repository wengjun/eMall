package com.emall.experiment;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
interface ExperimentMapper {
    @Insert("""
            INSERT INTO experiment_definition
                (experiment_id, scene, name, mutual_exclusion_group, traffic_percent, control_variant,
                treatment_variant, status, created_at, updated_at)
            VALUES (#{experiment.experimentId}, #{experiment.scene}, #{experiment.name},
                #{experiment.mutualExclusionGroup}, #{experiment.trafficPercent}, #{experiment.controlVariant},
                #{experiment.treatmentVariant}, #{experiment.status}, #{experiment.createdAt},
                #{experiment.updatedAt})
            ON DUPLICATE KEY UPDATE status = VALUES(status), updated_at = VALUES(updated_at)
            """)
    int saveExperiment(@Param("experiment") ExperimentDefinition experiment);
}
