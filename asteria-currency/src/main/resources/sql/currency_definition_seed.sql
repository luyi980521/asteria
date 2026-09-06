INSERT INTO currency_definition
    (code, numeric_code, display_name, symbol, minor_unit, enabled, created_at, updated_at)
VALUES
    ('USD', '840', 'US Dollar', '$', 2, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('EUR', '978', 'Euro', '€', 2, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('GBP', '826', 'Pound Sterling', '£', 2, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('JPY', '392', 'Japanese Yen', '¥', 0, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('CNY', '156', 'Chinese Yuan', '¥', 2, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
