package com.emall.common.archive;

public interface ArchiveRepository {
    int copyBatch(ArchiveRequest request);

    int deleteBatch(ArchiveRequest request);
}
