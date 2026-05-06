ALTER TABLE user_account
    MODIFY mobile VARCHAR(128) NOT NULL,
    ADD COLUMN mobile_ciphertext VARCHAR(1024) NULL AFTER mobile,
    ADD COLUMN mobile_hash VARCHAR(128) NULL AFTER mobile_ciphertext;

UPDATE user_account
SET mobile_hash = mobile
WHERE mobile_hash IS NULL;

CREATE UNIQUE INDEX uk_user_mobile_hash ON user_account (mobile_hash);
