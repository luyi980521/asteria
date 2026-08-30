package io.asteria.payment.entity;

import io.asteria.common.domain.valueobject.Money;
import io.asteria.payment.domain.error.PaymentErrorCode;
import io.asteria.payment.domain.exception.PaymentDomainException;
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
            throw new PaymentDomainException(PaymentErrorCode.PAYMENT_ID_MUST_BE_POSITIVE);
        }
        if (merchantId == null || merchantId <= 0) {
            throw new PaymentDomainException(PaymentErrorCode.MERCHANT_ID_MUST_BE_POSITIVE);
        }
        if (amount == null) {
            throw new PaymentDomainException(PaymentErrorCode.PAYMENT_AMOUNT_REQUIRED);
        }
        if (paymentMethod == null) {
            throw new PaymentDomainException(PaymentErrorCode.PAYMENT_METHOD_REQUIRED);
        }
        if (reference == null) {
            throw new PaymentDomainException(PaymentErrorCode.PAYMENT_REFERENCE_REQUIRED);
        }
        if (createdAt == null) {
            throw new PaymentDomainException(PaymentErrorCode.PAYMENT_CREATED_AT_REQUIRED);
        }

        return new Payment(paymentId, merchantId, amount, paymentMethod, reference,
                PaymentStatus.CREATED, createdAt, null, null);
    }

    /**
     * 标记支付进入授权处理中
     * */
    public void startAuthorization() {
        if (status != PaymentStatus.CREATED) {
            throw new PaymentDomainException(PaymentErrorCode.PAYMENT_MUST_BE_CREATED_TO_AUTHORIZE);
        }

        status = PaymentStatus.AUTHORIZING;
    }

    /**
     * 标记支付授权成功
     * */
    public void authorize(Instant authorizedAt) {
        if (status != PaymentStatus.AUTHORIZING) {
            throw new PaymentDomainException(PaymentErrorCode.PAYMENT_MUST_BE_AUTHORIZING_TO_AUTHORIZE);
        }
        if (authorizedAt == null) {
            throw new PaymentDomainException(PaymentErrorCode.PAYMENT_AUTHORIZED_AT_REQUIRED);
        }

        status = PaymentStatus.AUTHORIZED;
        this.authorizedAt = authorizedAt;
    }

    /**
     * 标记支付进入捕获处理中
     * */
    public void startCapture() {
        if (status != PaymentStatus.AUTHORIZED) {
            throw new PaymentDomainException(PaymentErrorCode.PAYMENT_MUST_BE_AUTHORIZED_TO_CAPTURE);
        }

        status = PaymentStatus.CAPTURING;
    }

    /**
     * 标记支付捕获成功
     * */
    public void capture(Instant capturedAt) {
        if (status != PaymentStatus.CAPTURING) {
            throw new PaymentDomainException(PaymentErrorCode.PAYMENT_MUST_BE_CAPTURING_TO_CAPTURE);
        }
        if (capturedAt == null) {
            throw new PaymentDomainException(PaymentErrorCode.PAYMENT_CAPTURED_AT_REQUIRED);
        }

        status = PaymentStatus.CAPTURED;
        this.capturedAt = capturedAt;
    }

    /**
     * 标记支付失败
     * */
    public void fail() {
        if (status == PaymentStatus.CAPTURED || status == PaymentStatus.CANCELLED) {
            throw new PaymentDomainException(PaymentErrorCode.PAYMENT_CANNOT_FAIL_IN_CURRENT_STATUS);
        }

        status = PaymentStatus.FAILED;
    }

    /**
     * 取消支付
     * */
    public void cancel() {
        if (status != PaymentStatus.CREATED && status != PaymentStatus.AUTHORIZED) {
            throw new PaymentDomainException(PaymentErrorCode.PAYMENT_CANNOT_CANCEL_IN_CURRENT_STATUS);
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
