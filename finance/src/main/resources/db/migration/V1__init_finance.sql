CREATE TABLE finance_account (
    account_id BIGINT PRIMARY KEY,
    account_type VARCHAR(64) NOT NULL,
    owner_id BIGINT NOT NULL,
    currency VARCHAR(16) NOT NULL,
    balance DECIMAL(19, 4) NOT NULL,
    frozen_amount DECIMAL(19, 4) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    KEY idx_finance_account_owner (owner_id, account_type)
);

CREATE TABLE ledger_entry (
    entry_id BIGINT PRIMARY KEY,
    account_id BIGINT NOT NULL,
    business_type VARCHAR(64) NOT NULL,
    business_no VARCHAR(128) NOT NULL,
    debit_amount DECIMAL(19, 4) NOT NULL,
    credit_amount DECIMAL(19, 4) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    KEY idx_ledger_entry_account (account_id),
    KEY idx_ledger_entry_business (business_type, business_no)
);

CREATE TABLE settlement_batch (
    batch_id BIGINT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    amount DECIMAL(19, 4) NOT NULL,
    commission_amount DECIMAL(19, 4) NOT NULL,
    status VARCHAR(32) NOT NULL,
    settlement_date DATE NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    KEY idx_settlement_batch_merchant (merchant_id)
);

CREATE TABLE invoice_document (
    invoice_id BIGINT PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    amount DECIMAL(19, 4) NOT NULL,
    tax_no VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    KEY idx_invoice_document_owner (owner_id)
);

CREATE TABLE clearing_file (
    clearing_file_id BIGINT PRIMARY KEY,
    channel VARCHAR(64) NOT NULL,
    clearing_date DATE NOT NULL,
    amount DECIMAL(19, 4) NOT NULL,
    balanced BOOLEAN NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    KEY idx_clearing_file_channel_date (channel, clearing_date)
);

CREATE TABLE chargeback_case (
    chargeback_id BIGINT PRIMARY KEY,
    payment_id BIGINT NOT NULL,
    amount DECIMAL(19, 4) NOT NULL,
    reason VARCHAR(512) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    KEY idx_chargeback_case_payment (payment_id),
    KEY idx_chargeback_case_status (status)
);
