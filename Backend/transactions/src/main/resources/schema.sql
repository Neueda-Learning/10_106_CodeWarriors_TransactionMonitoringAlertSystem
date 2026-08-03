-- ----------------------------------------------------
-- ACCOUNTS TABLE
-- ----------------------------------------------------
CREATE TABLE IF NOT EXISTS accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_name VARCHAR(100) NOT NULL,
    account_type VARCHAR(50) NOT NULL,
    country VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------
-- RULES TABLE
-- ----------------------------------------------------
CREATE TABLE IF NOT EXISTS rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(50) NOT NULL,
    threshold DECIMAL(15,2),
    time_window INT,
    max_transactions INT,
    severity VARCHAR(20),
    active BOOLEAN DEFAULT TRUE
);

-- ----------------------------------------------------
-- TRANSACTIONS TABLE
-- ----------------------------------------------------
CREATE TABLE IF NOT EXISTS bank_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    from_account_id BIGINT NOT NULL,
    to_account_id BIGINT NOT NULL,

    amount DECIMAL(15,2) NOT NULL,
    currency VARCHAR(10) NOT NULL,

    transaction_time TIMESTAMP NOT NULL,

    status VARCHAR(20) CHECK (status IN ('COMPLETED','PENDING','FAILED')),

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_from_account
        FOREIGN KEY (from_account_id)
        REFERENCES accounts(id),

    CONSTRAINT fk_to_account
        FOREIGN KEY (to_account_id)
        REFERENCES accounts(id)
);

-- ----------------------------------------------------
-- ALERTS TABLE
-- ----------------------------------------------------
CREATE TABLE IF NOT EXISTS alerts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    transaction_id BIGINT NOT NULL,
    rule_id BIGINT NOT NULL,

    alert_reason VARCHAR(255),
    severity VARCHAR(20),

    old_status VARCHAR(20),
    new_status VARCHAR(20),

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_alert_transaction
        FOREIGN KEY (transaction_id)
        REFERENCES bank_transactions(id),

    CONSTRAINT fk_alert_rule
        FOREIGN KEY (rule_id)
        REFERENCES rules(id)
);