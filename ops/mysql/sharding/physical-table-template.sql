-- Replace {table}, {index}, {source_table}, {shard_id}, and {cell_id} in the migration runner.
CREATE TABLE {table}_{index} LIKE {source_table};

ALTER TABLE {table}_{index}
    ADD CONSTRAINT chk_{table}_{index}_shard CHECK (shard_id = {shard_id}),
    ADD CONSTRAINT chk_{table}_{index}_cell CHECK (cell_id = '{cell_id}');
