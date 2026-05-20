package com.emall.common.archive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ArchiveServiceTest {
    @Test
    void shouldCopyThenDeleteOneShardBatch() {
        FakeArchiveRepository repository = new FakeArchiveRepository(100);
        ArchiveService service = new ArchiveService(repository);
        ArchiveRequest request =
                new ArchiveRequest("order_record", "order_record_archive", 3, LocalDate.parse("2026-01-01"), 500);

        ArchiveResult result = service.archiveBatch(request);

        assertThat(result.copiedRows()).isEqualTo(100);
        assertThat(result.deletedRows()).isEqualTo(100);
        assertThat(repository.copyCalled).isTrue();
        assertThat(repository.deleteCalled).isTrue();
    }

    @Test
    void shouldRejectUnboundedBatchSize() {
        assertThatThrownBy(() -> new ArchiveRequest("payment_order", "payment_order_archive", 0,
                LocalDate.parse("2026-01-01"), 20000)).isInstanceOf(IllegalArgumentException.class);
    }

    private static final class FakeArchiveRepository implements ArchiveRepository {
        private final int rows;
        private boolean copyCalled;
        private boolean deleteCalled;

        private FakeArchiveRepository(int rows) {
            this.rows = rows;
        }

        @Override
        public int copyBatch(ArchiveRequest request) {
            copyCalled = true;
            return rows;
        }

        @Override
        public int deleteBatch(ArchiveRequest request) {
            deleteCalled = true;
            return rows;
        }
    }
}
