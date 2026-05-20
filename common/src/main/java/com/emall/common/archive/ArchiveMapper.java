package com.emall.common.archive;

import org.apache.ibatis.annotations.DeleteProvider;
import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.Param;

public interface ArchiveMapper {
    @InsertProvider(type = ArchiveSqlProvider.class, method = "copyBatch")
    int copyBatch(@Param("request") ArchiveRequest request);

    @DeleteProvider(type = ArchiveSqlProvider.class, method = "deleteBatch")
    int deleteBatch(@Param("request") ArchiveRequest request);
}
