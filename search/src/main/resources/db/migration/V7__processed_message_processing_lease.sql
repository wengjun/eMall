ALTER TABLE processed_message
    ADD KEY idx_processed_message_processing_reclaim (status, updated_at);
