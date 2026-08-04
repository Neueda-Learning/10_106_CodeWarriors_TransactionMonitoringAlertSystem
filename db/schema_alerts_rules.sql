-- Table for Monitoring Rules
CREATE TABLE monitoring_rules (
    rule_id INT AUTO_INCREMENT PRIMARY KEY,
    rule_name VARCHAR(100) NOT NULL,
    rule_type VARCHAR(50) NOT NULL, -- 'AMOUNT_THRESHOLD', 'VELOCITY', 'NEW_PAYEE'
    severity VARCHAR(20) NOT NULL, -- 'LOW', 'MEDIUM', 'HIGH'
    threshold_amount DECIMAL(15,2),
    time_window_minutes INT,
    max_transactions INT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table for Alerts
CREATE TABLE alerts (
    alert_id VARCHAR(50) PRIMARY KEY,
    transaction_id VARCHAR(50) NOT NULL,
    rule_id INT NOT NULL,
    status VARCHAR(20) DEFAULT 'OPEN', -- 'OPEN', 'ACKNOWLEDGED', 'INVESTIGATING', 'CLOSED', 'DISMISSED'
    severity VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    acknowledged_at TIMESTAMP NULL,
    closed_at TIMESTAMP NULL,
    resolution_notes TEXT,
    FOREIGN KEY (transaction_id) REFERENCES transactions(transaction_id),
    FOREIGN KEY (rule_id) REFERENCES monitoring_rules(rule_id)
);

-- Insert initial rules
INSERT INTO monitoring_rules (rule_name, rule_type, severity, threshold_amount)
VALUES ('Large Transaction Threshold', 'AMOUNT_THRESHOLD', 'HIGH', 10000.00);

INSERT INTO monitoring_rules (rule_name, rule_type, severity, time_window_minutes, max_transactions)
VALUES ('High Velocity Check', 'VELOCITY', 'MEDIUM', 10, 5);
