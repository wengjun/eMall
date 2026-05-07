package com.emall.supplychain;

import static org.assertj.core.api.Assertions.assertThat;

import com.emall.common.id.SnowflakeIdGenerator;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class SupplyChainServiceTest {
    private final InMemorySupplyChainRepository repository = new InMemorySupplyChainRepository();
    private final SupplyChainService service = new SupplyChainService(repository, new SnowflakeIdGenerator(41L));

    @Test
    void managesWarehouseReceiptTransferAndWaybill() {
        WarehouseReceipt receipt =
                service.receiveStock(1001L, "WH-EAST", "BATCH-1", 100, LocalDate.now().plusDays(365));
        WarehouseReceipt shelved = service.shelve(receipt.receiptId());
        InventoryTransfer transfer = service.createTransfer(1001L, "WH-EAST", "WH-SOUTH", 20);
        LogisticsWaybill waybill = service.createWaybill(9001L, "carrier-a", "east-south", 48);
        service.reportDeliveryException(waybill.waybillId(), "delivery address changed");
        LogisticsWaybill delivered = service.confirmDelivery(waybill.waybillId());

        assertThat(shelved.status()).isEqualTo(ReceiptStatus.SHELVED);
        assertThat(transfer.status()).isEqualTo(TransferStatus.CREATED);
        assertThat(delivered.status()).isEqualTo(WaybillStatus.DELIVERED);
        assertThat(service.summary("WH-EAST").receipts()).isEqualTo(1);
    }
}
