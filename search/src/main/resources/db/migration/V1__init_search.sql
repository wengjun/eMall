CREATE TABLE search_document (
    sku_id BIGINT NOT NULL,
    title VARCHAR(256) NOT NULL,
    category VARCHAR(128) NOT NULL,
    price DECIMAL(18, 2) NOT NULL,
    tags VARCHAR(1024) NOT NULL,
    saleable BOOLEAN NOT NULL,
    indexed_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (sku_id),
    KEY idx_search_saleable_indexed (saleable, indexed_at),
    FULLTEXT KEY ft_search_text (title, category, tags)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO search_document (sku_id, title, category, price, tags, saleable, indexed_at)
VALUES
    (10001, 'flagship phone', 'digital', 3799.00, 'phone,mobile', true, UTC_TIMESTAMP(6)),
    (10002, 'thin laptop', 'computer', 6799.00, 'laptop,computer', true, UTC_TIMESTAMP(6));
