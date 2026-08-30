package io.asteria.payment.entity;

import io.asteria.common.domain.valueobject.Money;
import io.asteria.payment.domain.entity.Payment;
import io.asteria.payment.domain.error.PaymentErrorCode;
import io.asteria.payment.domain.exception.PaymentDomainException;
import io.asteria.payment.domain.enums.PaymentMethod;
import io.asteria.payment.domain.enums.PaymentStatus;
import io.asteria.payment.domain.valueobject.PaymentId;
import io.asteria.payment.domain.valueobject.PaymentReference;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaymentTest {

    private static final PaymentId PAYMENT_ID = PaymentId.of(1001L);
    private static final Money AMOUNT = Money.of(new BigDecimal("12.34"), Currency.getInstance("USD"));
    private static final PaymentReference REFERENCE = new PaymentReference("ORDER", "order-1001");
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant AUTHORIZED_AT = Instant.parse("2026-01-01T00:01:00Z");
    private static final Instant CAPTURED_AT = Instant.parse("2026-01-01T00:02:00Z");

    @Test
    void createsPaymentInCreatedStatus() {
        Payment payment = payment();

        assertEquals(PAYMENT_ID, payment.getPaymentId());
        assertEquals(42L, payment.getMerchantId());
        assertEquals(AMOUNT, payment.getAmount());
        assertEquals(PaymentStatus.CREATED, payment.getStatus());
        assertEquals(CREATED_AT, payment.getCreatedAt());
    }

    @Test
    void followsAuthorizationAndCaptureStateMachine() {
        Payment payment = payment();

        payment.startAuthorization();
        assertEquals(PaymentStatus.AUTHORIZING, payment.getStatus());
        payment.authorize(AUTHORIZED_AT);
        assertEquals(PaymentStatus.AUTHORIZED, payment.getStatus());
        assertEquals(AUTHORIZED_AT, payment.getAuthorizedAt());
        payment.startCapture();
        assertEquals(PaymentStatus.CAPTURING, payment.getStatus());
        payment.capture(CAPTURED_AT);
        assertEquals(PaymentStatus.CAPTURED, payment.getStatus());
        assertEquals(CAPTURED_AT, payment.getCapturedAt());
    }

    @Test
    void canCancelFromCreatedOrAuthorized() {
        Payment created = payment();
        created.cancel();
        assertEquals(PaymentStatus.CANCELLED, created.getStatus());

        Payment authorized = payment();
        authorized.startAuthorization();
        authorized.authorize(AUTHORIZED_AT);
        authorized.cancel();
        assertEquals(PaymentStatus.CANCELLED, authorized.getStatus());
    }

    @Test
    void canFailFromNonTerminalStates() {
        Payment created = payment();
        created.fail();
        assertEquals(PaymentStatus.FAILED, created.getStatus());

        Payment authorizing = payment();
        authorizing.startAuthorization();
        authorizing.fail();
        assertEquals(PaymentStatus.FAILED, authorizing.getStatus());
    }

    @Test
    void rejectsIllegalStateTransitions() {
        Payment payment = payment();

        assertError(PaymentErrorCode.PAYMENT_MUST_BE_AUTHORIZING_TO_AUTHORIZE,
                () -> payment.authorize(AUTHORIZED_AT));
        assertError(PaymentErrorCode.PAYMENT_MUST_BE_AUTHORIZED_TO_CAPTURE,
                payment::startCapture);
        assertError(PaymentErrorCode.PAYMENT_MUST_BE_CAPTURING_TO_CAPTURE,
                () -> payment.capture(CAPTURED_AT));

        payment.startAuthorization();
        assertError(PaymentErrorCode.PAYMENT_MUST_BE_CREATED_TO_AUTHORIZE,
                payment::startAuthorization);
        payment.authorize(AUTHORIZED_AT);
        payment.startCapture();
        assertError(PaymentErrorCode.PAYMENT_CANNOT_CANCEL_IN_CURRENT_STATUS, payment::cancel);
        payment.capture(CAPTURED_AT);
        assertError(PaymentErrorCode.PAYMENT_CANNOT_FAIL_IN_CURRENT_STATUS, payment::fail);
    }

    @Test
    void rejectsInvalidPaymentIdAndCreationArguments() {
        assertError(PaymentErrorCode.PAYMENT_ID_MUST_BE_POSITIVE, () -> PaymentId.of(null));
        assertError(PaymentErrorCode.PAYMENT_ID_MUST_BE_POSITIVE, () -> PaymentId.of(0L));
        assertError(PaymentErrorCode.PAYMENT_ID_MUST_BE_POSITIVE, () -> PaymentId.of(-1L));
        assertError(PaymentErrorCode.PAYMENT_ID_MUST_BE_POSITIVE,
                () -> Payment.create(null, 42L, AMOUNT, PaymentMethod.CARD, REFERENCE, CREATED_AT));
        assertError(PaymentErrorCode.MERCHANT_ID_MUST_BE_POSITIVE,
                () -> Payment.create(PAYMENT_ID, null, AMOUNT, PaymentMethod.CARD, REFERENCE, CREATED_AT));
        assertError(PaymentErrorCode.MERCHANT_ID_MUST_BE_POSITIVE,
                () -> Payment.create(PAYMENT_ID, 0L, AMOUNT, PaymentMethod.CARD, REFERENCE, CREATED_AT));
        assertError(PaymentErrorCode.PAYMENT_AMOUNT_REQUIRED,
                () -> Payment.create(PAYMENT_ID, 42L, null, PaymentMethod.CARD, REFERENCE, CREATED_AT));
        assertError(PaymentErrorCode.PAYMENT_METHOD_REQUIRED,
                () -> Payment.create(PAYMENT_ID, 42L, AMOUNT, null, REFERENCE, CREATED_AT));
        assertError(PaymentErrorCode.PAYMENT_REFERENCE_REQUIRED,
                () -> Payment.create(PAYMENT_ID, 42L, AMOUNT, PaymentMethod.CARD, null, CREATED_AT));
        assertError(PaymentErrorCode.PAYMENT_CREATED_AT_REQUIRED,
                () -> Payment.create(PAYMENT_ID, 42L, AMOUNT, PaymentMethod.CARD, REFERENCE, null));
    }

    @Test
    void rejectsInvalidReferenceAndTransitionTimes() {
        assertError(PaymentErrorCode.PAYMENT_REFERENCE_TYPE_REQUIRED,
                () -> new PaymentReference(" ", "id"));
        assertError(PaymentErrorCode.PAYMENT_REFERENCE_ID_REQUIRED,
                () -> new PaymentReference("type", ""));

        Payment payment = payment();
        payment.startAuthorization();
        assertError(PaymentErrorCode.PAYMENT_AUTHORIZED_AT_REQUIRED, () -> payment.authorize(null));
        payment.authorize(AUTHORIZED_AT);
        payment.startCapture();
        assertError(PaymentErrorCode.PAYMENT_CAPTURED_AT_REQUIRED, () -> payment.capture(null));
    }

    @Test
    void reconstitutesCompletePaymentState() {
        Payment payment = Payment.reconstitute(PAYMENT_ID, 42L, AMOUNT, PaymentMethod.WALLET,
                REFERENCE, PaymentStatus.CAPTURED, CREATED_AT, AUTHORIZED_AT, CAPTURED_AT);

        assertEquals(PAYMENT_ID, payment.getPaymentId());
        assertEquals(42L, payment.getMerchantId());
        assertEquals(AMOUNT, payment.getAmount());
        assertEquals(PaymentMethod.WALLET, payment.getPaymentMethod());
        assertEquals(REFERENCE, payment.getReference());
        assertEquals(PaymentStatus.CAPTURED, payment.getStatus());
        assertEquals(CREATED_AT, payment.getCreatedAt());
        assertEquals(AUTHORIZED_AT, payment.getAuthorizedAt());
        assertEquals(CAPTURED_AT, payment.getCapturedAt());
    }

    private static Payment payment() {
        return Payment.create(PAYMENT_ID, 42L, AMOUNT, PaymentMethod.CARD, REFERENCE, CREATED_AT);
    }

    private static void assertError(PaymentErrorCode expected, Runnable action) {
        PaymentDomainException exception = assertThrows(PaymentDomainException.class, action::run);
        assertEquals(expected, exception.errorCode());
    }
}
