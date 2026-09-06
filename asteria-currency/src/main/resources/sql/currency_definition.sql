CREATE TABLE currency_definition (
    code VARCHAR(3) PRIMARY KEY,
    numeric_code VARCHAR(3),
    display_name VARCHAR(64) NOT NULL,
    symbol VARCHAR(16),
    minor_unit SMALLINT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT ck_currency_definition_code
        CHECK (code ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_currency_definition_numeric_code
        CHECK (numeric_code IS NULL OR numeric_code ~ '^[0-9]{3}$'),
    CONSTRAINT ck_currency_definition_minor_unit
        CHECK (minor_unit >= 0 AND minor_unit <= 4)
);
