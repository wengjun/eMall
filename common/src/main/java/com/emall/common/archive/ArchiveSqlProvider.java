package com.emall.common.archive;

import java.util.Map;
import java.util.regex.Pattern;

public class ArchiveSqlProvider {
    private static final Pattern TABLE_NAME = Pattern.compile("[a-zA-Z][a-zA-Z0-9_]*");

    public String copyBatch(Map<String, Object> parameters) {
        ArchiveRequest request = request(parameters);
        return """
                INSERT IGNORE INTO %s
                SELECT source_rows.*
                FROM %s source_rows
                WHERE source_rows.shard_id = #{request.shardId}
                  AND source_rows.biz_date < #{request.cutoffDate}
                ORDER BY source_rows.biz_date
                LIMIT #{request.batchSize}
                """.formatted(table(request.archiveTable()), table(request.sourceTable()));
    }

    public String deleteBatch(Map<String, Object> parameters) {
        ArchiveRequest request = request(parameters);
        return """
                DELETE FROM %s
                WHERE shard_id = #{request.shardId}
                  AND biz_date < #{request.cutoffDate}
                ORDER BY biz_date
                LIMIT #{request.batchSize}
                """.formatted(table(request.sourceTable()));
    }

    private ArchiveRequest request(Map<String, Object> parameters) {
        return (ArchiveRequest) parameters.get("request");
    }

    private String table(String table) {
        if (!TABLE_NAME.matcher(table).matches()) {
            throw new IllegalArgumentException("invalid archive table name: " + table);
        }
        return table;
    }
}
