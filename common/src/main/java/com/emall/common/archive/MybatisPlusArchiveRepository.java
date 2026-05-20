package com.emall.common.archive;

public class MybatisPlusArchiveRepository implements ArchiveRepository {
    private final ArchiveMapper archiveMapper;

    public MybatisPlusArchiveRepository(ArchiveMapper archiveMapper) {
        this.archiveMapper = archiveMapper;
    }

    @Override
    public int copyBatch(ArchiveRequest request) {
        return archiveMapper.copyBatch(request);
    }

    @Override
    public int deleteBatch(ArchiveRequest request) {
        return archiveMapper.deleteBatch(request);
    }
}
