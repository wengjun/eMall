package com.emall.common.sharding;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;

public final class ShardWriteFencedException extends BusinessException {
    public ShardWriteFencedException(String namespace, int virtualShard, long mappingVersion, long epoch,
            ShardMigrationState state) {
        super(ErrorCode.CONFLICT, "virtual shard write is fenced, namespace=%s shard=%d version=%d epoch=%d state=%s"
                .formatted(namespace, virtualShard, mappingVersion, epoch, state));
    }
}
