package com.emall.inventory.repository;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InventoryBucketSummaryProjection {
    private Long total;
    private Long reserved;
    private Long sold;
    private Long bucketCount;
    private LocalDateTime updatedAt;
}
