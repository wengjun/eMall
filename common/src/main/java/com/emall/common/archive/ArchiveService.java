package com.emall.common.archive;

public class ArchiveService {
    private final ArchiveRepository archiveRepository;

    public ArchiveService(ArchiveRepository archiveRepository) {
        this.archiveRepository = archiveRepository;
    }

    public ArchiveResult archiveBatch(ArchiveRequest request) {
        int copiedRows = archiveRepository.copyBatch(request);
        int deletedRows = copiedRows == 0 ? 0 : archiveRepository.deleteBatch(request);
        return new ArchiveResult(request, copiedRows, deletedRows);
    }
}
