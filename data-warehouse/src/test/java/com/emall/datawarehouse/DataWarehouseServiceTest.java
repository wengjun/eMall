package com.emall.datawarehouse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emall.common.exception.BusinessException;
import com.emall.common.id.SnowflakeIdGenerator;
import com.emall.common.privacy.SensitiveDataType;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DataWarehouseServiceTest {
    private final InMemoryDataWarehouseRepository repository = new InMemoryDataWarehouseRepository();
    private final DataWarehouseService service = new DataWarehouseService(repository, new SnowflakeIdGenerator(52L));

    @Test
    void registersDatasetsPartitionsQualityAndLineage() {
        DatasetDefinition ods = service.registerDataset(WarehouseLayer.ODS, "ods_order", "data", "raw order", 30);
        DatasetDefinition dws = service.registerDataset(WarehouseLayer.DWS, "dws_order", "data", "summary", 365);
        service.addPartition(ods.datasetId(), "dt", LocalDate.now(), 1000, 10_000);
        service.recordQualityCheck(ods.datasetId(), "row_count", QualityStatus.PASS, "ok");
        service.recordQualityCheck(dws.datasetId(), "freshness", QualityStatus.FAIL, "late");
        service.addLineage(ods.datasetId(), dws.datasetId(), "order_summary_job");
        service.addFieldLineage(ods.datasetId(), "buyer_mobile", dws.datasetId(), "buyer_mobile_hash",
                SensitiveDataType.MOBILE, "hash_mobile");

        WarehouseSummary summary = service.summary();

        assertThat(summary.datasets()).isEqualTo(2);
        assertThat(summary.partitions()).isEqualTo(1);
        assertThat(summary.failedChecks()).isEqualTo(1);
        assertThat(summary.lineageEdges()).isEqualTo(1);
        assertThat(summary.fieldLineageEdges()).isEqualTo(1);
        assertThat(summary.openQualityAlerts()).isEqualTo(1);
    }

    @Test
    void blocksReportPartitionWhenQualityFailed() {
        DatasetDefinition ads = service.registerDataset(WarehouseLayer.ADS, "ads_order_report", "data", "report", 365);
        service.recordQualityCheck(ads.datasetId(), "freshness", QualityStatus.FAIL, "late");

        assertThatThrownBy(() -> service.addPartition(ads.datasetId(), "dt", LocalDate.now(), 1, 1))
                .isInstanceOf(BusinessException.class).hasMessageContaining("quality check blocks");
    }
}
