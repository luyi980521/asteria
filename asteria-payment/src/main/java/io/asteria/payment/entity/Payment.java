package io.asteria.payment.entity;

import io.asteria.common.domain.valueobject.Money;
import io.asteria.payment.enums.PaymentMethod;
import io.asteria.payment.enums.PaymentStatus;
import io.asteria.payment.valueobject.PaymentId;
import io.asteria.payment.valueobject.PaymentReference;

import java.time.Instant;

/**
 * 支付值对象定义
 * */
public class Payment {

    /** 支付ID */
    private final PaymentId paymentId;

    /** 商户ID */
    private final Long merchantId;

    /** 支付金额 */
    private final Money amount;

    /** 支付方式 */
    private final PaymentMethod paymentMethod;

    /** 支付业务关联信息 */
    private final PaymentReference reference;

    /** 支付状态 */
    private PaymentStatus status;

    /** 创建时间 */
    private final Instant createdAt;

    /** 授权成功时间 */
    private Instant authorizedAt;

    /** 捕获成功时间 */
    private Instant capturedAt;

    private Payment(PaymentId paymentId, Long merchantId, Money amount,
                    PaymentMethod paymentMethod, PaymentReference reference,
                    PaymentStatus status, Instant createdAt,
                    Instant authorizedAt, Instant capturedAt) {
        this.paymentId = paymentId;
        this.merchantId = merchantId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.reference = reference;
        this.status = status;
        this.createdAt = createdAt;
        this.authorizedAt = authorizedAt;
        this.capturedAt = capturedAt;
    }

    /**
     * 创建支付
     * */
    public static Payment create(PaymentId paymentId, Long merchantId, Money amount,
                                 PaymentMethod paymentMethod, PaymentReference reference,
                                 Instant createdAt) {
        if (paymentId == null) {
            throw new IllegalArgumentException("Payment id must not be null");
        }
        if (merchantId == null || merchantId <= 0) {
            throw new IllegalArgumentException("Merchant id must be positive");
        }
        if (amount == null) {
            throw new IllegalArgumentException("Payment amount must not be null");
        }
        if (paymentMethod == null) {
            throw new IllegalArgumentException("Payment method must not be null");
        }
        if (reference == null) {
            throw new IllegalArgumentException("Payment reference must not be null");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("Created time must not be null");
        }

        return new Payment(paymentId, merchantId, amount, paymentMethod, reference,
                PaymentStatus.CREATED, createdAt, null, null);
    }

    /**
     * 标记支付进入授权处理中
     * */
    public void startAuthorization() {
        if (status != PaymentStatus.CREATED) {
            throw new IllegalStateException("Only created payment can start authorization");
        }

        status = PaymentStatus.AUTHORIZING;
    }

    /**
     * 标记支付授权成功
     * */
    public void authorize(Instant authorizedAt) {
        if (status != PaymentStatus.AUTHORIZING) {
            throw new IllegalStateException("Only authorizing payment can be authorized");
        }
        if (authorizedAt == null) {
            throw new IllegalArgumentException("Authorized time must not be null");
        }

        status = PaymentStatus.AUTHORIZED;
        this.authorizedAt = authorizedAt;
    }

    /**
     * 标记支付进入捕获处理中
     * */
    public void startCapture() {
        if (status != PaymentStatus.AUTHORIZED) {
            throw new IllegalStateException("Only authorized payment can start capture");
        }

        status = PaymentStatus.CAPTURING;
    }

    /**
     * 标记支付捕获成功
     * */
    public void capture(Instant capturedAt) {
        if (status != PaymentStatus.CAPTURING) {
            throw new IllegalStateException("Only capturing payment can be captured");
        }
        if (capturedAt == null) {
            throw new IllegalArgumentException("Captured time must not be null");
        }

        status = PaymentStatus.CAPTURED;
        this.capturedAt = capturedAt;
    }

    /**
     * 标记支付失败
     * */
    public void fail() {
        if (status == PaymentStatus.CAPTURED || status == PaymentStatus.CANCELLED) {
            throw new IllegalStateException("Current payment cannot be marked as failed");
        }

        status = PaymentStatus.FAILED;
    }

    /**
     * 取消支付
     * */
    public void cancel() {
        if (status != PaymentStatus.CREATED && status != PaymentStatus.AUTHORIZED) {
            throw new IllegalStateException("Current payment cannot be cancelled");
        }

        status = PaymentStatus.CANCELLED;
    }

    /**
     * 从持久化数据恢复支付
     * */
    public static Payment reconstitute(PaymentId paymentId, Long merchantId, Money amount,
                                       PaymentMethod paymentMethod, PaymentReference reference,
                                       PaymentStatus status, Instant createdAt,
                                       Instant authorizedAt, Instant capturedAt) {
        return new Payment(paymentId, merchantId, amount, paymentMethod, reference,
                status, createdAt, authorizedAt, capturedAt);
    }

    /** 获取支付ID */
    public PaymentId getPaymentId() {
        return paymentId;
    }

    /** 获取商户ID */
    public Long getMerchantId() {
        return merchantId;
    }

    /** 获取支付金额 */
    public Money getAmount() {
        return amount;
    }

    /** 获取支付方式 */
    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    /** 获取支付业务关联信息 */
    public PaymentReference getReference() {
        return reference;
    }

    /** 获取支付状态 */
    public PaymentStatus getStatus() {
        return status;
    }

    /** 获取创建时间 */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /** 获取授权成功时间 */
    public Instant getAuthorizedAt() {
        return authorizedAt;
    }

    /** 获取捕获成功时间 */
    public Instant getCapturedAt() {
        return capturedAt;
    }
}
