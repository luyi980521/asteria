package io.asteria.ledger.domain.valueobject;

/**
 * 事件ID
 */
public record EventId(Long value) {

    /**
     * 创建事件ID
     */
    public static EventId of(Long value) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("Event id must be positive");
        }

        return new EventId(value);
    }
}