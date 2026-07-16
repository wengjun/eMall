package com.emall.common.outbox;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface OutboxEventMapper extends BaseMapper<OutboxEventRecord> {
    @Insert("""
            INSERT INTO outbox_aggregate_sequence (aggregate_key, aggregate_version, updated_at)
            VALUES (#{aggregateKey}, 1, #{updatedAt})
            ON DUPLICATE KEY UPDATE aggregate_version = aggregate_version + 1, updated_at = #{updatedAt}
            """)
    int advanceAggregateVersion(@Param("aggregateKey") String aggregateKey,
            @Param("updatedAt") LocalDateTime updatedAt);

    @Select("""
            SELECT aggregate_version
            FROM outbox_aggregate_sequence
            WHERE aggregate_key = #{aggregateKey}
            """)
    Long currentAggregateVersion(@Param("aggregateKey") String aggregateKey);

    @Select("""
            SELECT candidate.*
            FROM outbox_event candidate
            WHERE (
                (candidate.status IN ('NEW', 'FAILED') AND candidate.next_retry_at <= #{now})
                OR (candidate.status = 'PROCESSING' AND candidate.claimed_until <= #{now})
            )
            AND NOT EXISTS (
                SELECT 1
                FROM outbox_event earlier
                WHERE earlier.aggregate_type = candidate.aggregate_type
                  AND earlier.aggregate_id = candidate.aggregate_id
                  AND earlier.status NOT IN ('PUBLISHED', 'DEAD')
                  AND (
                      (COALESCE(candidate.aggregate_version, 0) > 0
                       AND COALESCE(earlier.aggregate_version, 0) < candidate.aggregate_version)
                      OR (COALESCE(candidate.aggregate_version, 0) = 0
                          AND COALESCE(earlier.aggregate_version, 0) = 0
                          AND (earlier.created_at < candidate.created_at
                               OR (earlier.created_at = candidate.created_at
                                   AND earlier.event_id < candidate.event_id)))
                  )
            )
            ORDER BY candidate.shard_id, candidate.aggregate_type, candidate.aggregate_id,
                     candidate.aggregate_version, candidate.created_at
            LIMIT #{limit}
            """)
    List<OutboxEventRecord> selectPublishableHeads(@Param("now") LocalDateTime now, @Param("limit") int limit);
}
