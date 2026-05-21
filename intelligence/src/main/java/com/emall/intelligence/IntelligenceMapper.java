package com.emall.intelligence;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
interface IntelligenceMapper {
    @Insert("""
            INSERT INTO user_profile
                (profile_id, user_id, segment, preferences, privacy_restricted, updated_at)
            VALUES (#{profile.profileId}, #{profile.userId}, #{profile.segment}, #{profile.preferences},
                #{profile.privacyRestricted}, #{profile.updatedAt})
            ON DUPLICATE KEY UPDATE segment = VALUES(segment), preferences = VALUES(preferences),
                privacy_restricted = VALUES(privacy_restricted), updated_at = VALUES(updated_at)
            """)
    int saveUserProfile(@Param("profile") UserProfile profile);

    @Insert("""
            INSERT INTO item_profile
                (profile_id, sku_id, category, attributes, quality_score, updated_at)
            VALUES (#{profile.profileId}, #{profile.skuId}, #{profile.category}, #{profile.attributes},
                #{profile.qualityScore}, #{profile.updatedAt})
            ON DUPLICATE KEY UPDATE category = VALUES(category), attributes = VALUES(attributes),
                quality_score = VALUES(quality_score), updated_at = VALUES(updated_at)
            """)
    int saveItemProfile(@Param("profile") ItemProfile profile);

    @Insert("""
            INSERT INTO model_deployment
                (model_id, model_name, version, use_case, status, approval_ticket, created_at, updated_at)
            VALUES (#{model.modelId}, #{model.modelName}, #{model.version}, #{model.useCase}, #{model.status},
                #{model.approvalTicket}, #{model.createdAt}, #{model.updatedAt})
            ON DUPLICATE KEY UPDATE status = VALUES(status), approval_ticket = VALUES(approval_ticket),
                updated_at = VALUES(updated_at)
            """)
    int saveModel(@Param("model") ModelDeployment model);
}
