CREATE TABLE service_ticket (
    ticket_id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    category VARCHAR(64) NOT NULL,
    priority VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    assignee VARCHAR(128) NOT NULL DEFAULT '',
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    KEY idx_service_ticket_user (user_id),
    KEY idx_service_ticket_status (status)
);

CREATE TABLE arbitration_case (
    arbitration_id BIGINT PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    merchant_id BIGINT NOT NULL,
    reason VARCHAR(512) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    KEY idx_arbitration_case_ticket (ticket_id),
    KEY idx_arbitration_case_status (status)
);

CREATE TABLE compensation_record (
    compensation_id BIGINT PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    amount DECIMAL(19, 4) NOT NULL,
    reason VARCHAR(512) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    KEY idx_compensation_record_ticket (ticket_id)
);

CREATE TABLE knowledge_article (
    article_id BIGINT PRIMARY KEY,
    category VARCHAR(64) NOT NULL,
    title VARCHAR(256) NOT NULL,
    content TEXT NOT NULL,
    published BOOLEAN NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    KEY idx_knowledge_article_category (category)
);

CREATE TABLE service_quality_review (
    review_id BIGINT PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    score INT NOT NULL,
    comment VARCHAR(512) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    KEY idx_service_quality_review_ticket (ticket_id)
);
