package com.emall.datawarehouse;

import static org.assertj.core.api.Assertions.assertThat;

import com.emall.common.id.SnowflakeIdGenerator;
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

        WarehouseSummary summary = service.summary();

        assertThat(summary.datasets()).isEqualTo(2);
        assertThat(summary.partitions()).isEqualTo(1);
        assertThat(summary.failedChecks()).isEqualTo(1);
        assertThat(summary.lineageEdges()).isEqualTo(1);
    }
}
