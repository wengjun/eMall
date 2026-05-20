package com.emall.common.archive;

public record ArchiveResult(ArchiveRequest request, int copiedRows, int deletedRows) {
    public boolean completedBatch() {
        return copiedRows == 0 && deletedRows == 0;
    }
}
