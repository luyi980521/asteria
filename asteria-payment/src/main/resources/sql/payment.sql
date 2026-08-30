CREATE TABLE payment (
    id                           BIGINT PRIMARY KEY,
    merchant_id                  BIGINT          NOT NULL,
    amount                       NUMERIC(38, 18) NOT NULL,
    currency                     VARCHAR(16)     NOT NULL,
    payment_method               VARCHAR(32)     NOT NULL,
    reference_type               VARCHAR(64)     NOT NULL,
    reference_id                 VARCHAR(128)    NOT NULL,
    status                       VARCHAR(32)     NOT NULL,
    authorization_transaction_id VARCHAR(128)    NULL,
    created_at                   TIMESTAMPTZ     NOT NULL,
    authorized_at                TIMESTAMPTZ     NULL,
    captured_at                  TIMESTAMPTZ     NULL,
    updated_at                   TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version                      BIGINT          NOT NULL DEFAULT 0,

    CONSTRAINT ck_payment_id_positive
        CHECK (id > 0),

    CONSTRAINT ck_payment_merchant_id_positive
        CHECK (merchant_id > 0),

    CONSTRAINT ck_payment_amount_positive
        CHECK (amount > 0),

    CONSTRAINT ck_payment_currency_not_blank
        CHECK (length(trim(currency)) > 0),

    CONSTRAINT ck_payment_method
        CHECK (
            payment_method IN (
                'CARD',
                'BANK_TRANSFER',
                'WALLET'
            )
        ),

    CONSTRAINT ck_payment_status
        CHECK (
            status IN (
                'CREATED',
                'AUTHORIZING',
                'AUTHORIZED',
                'CAPTURING',
                'CAPTURED',
                'FAILED',
                'CANCELLED'
            )
        ),

    CONSTRAINT ck_payment_reference_type_not_blank
        CHECK (length(trim(reference_type)) > 0),

    CONSTRAINT ck_payment_reference_id_not_blank
        CHECK (length(trim(reference_id)) > 0),

    CONSTRAINT uk_payment_reference
        UNIQUE (reference_type, reference_id),

    CONSTRAINT ck_payment_version_non_negative
        CHECK (version >= 0),

    CONSTRAINT ck_payment_authorized_at
        CHECK (
            status NOT IN ('AUTHORIZED', 'CAPTURING', 'CAPTURED')
            OR authorized_at IS NOT NULL
        ),

    CONSTRAINT ck_payment_captured_at
        CHECK (
            status <> 'CAPTURED'
            OR captured_at IS NOT NULL
        ),

    CONSTRAINT ck_payment_capture_after_authorization
        CHECK (
            captured_at IS NULL
            OR authorized_at IS NULL
            OR captured_at >= authorized_at
        )
);

COMMENT ON TABLE payment
    IS '支付主表';

COMMENT ON COLUMN payment.id
    IS '支付ID，由应用层雪花算法生成';

COMMENT ON COLUMN payment.merchant_id
    IS '所属商户ID';

COMMENT ON COLUMN payment.amount
    IS '支付金额';

COMMENT ON COLUMN payment.currency
    IS '支付币种，例如 USD、EUR、CNY';

COMMENT ON COLUMN payment.payment_method
    IS '支付方式：CARD、BANK_TRANSFER、WALLET';

COMMENT ON COLUMN payment.reference_type
    IS '上游业务关联类型，例如 MERCHANT_ORDER';

COMMENT ON COLUMN payment.reference_id
    IS '上游业务关联ID，例如商户订单号';

COMMENT ON COLUMN payment.status
    IS '支付状态：CREATED、AUTHORIZING、AUTHORIZED、CAPTURING、CAPTURED、FAILED、CANCELLED';

COMMENT ON COLUMN payment.authorization_transaction_id
    IS '支付渠道授权交易ID';

COMMENT ON COLUMN payment.created_at
    IS '支付创建时间';

COMMENT ON COLUMN payment.authorized_at
    IS '授权成功时间';

COMMENT ON COLUMN payment.captured_at
    IS '捕获成功时间';

COMMENT ON COLUMN payment.updated_at
    IS '数据库记录最后更新时间';

COMMENT ON COLUMN payment.version
    IS '乐观锁版本号';


CREATE INDEX idx_payment_merchant_created_at
    ON payment (merchant_id, created_at DESC);

CREATE INDEX idx_payment_status_created_at
    ON payment (status, created_at DESC);

CREATE INDEX idx_payment_created_at
    ON payment (created_at DESC);