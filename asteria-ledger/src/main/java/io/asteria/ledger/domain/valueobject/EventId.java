package io.asteria.ledger.domain.valueobject;

import org.apache.commons.lang3.StringUtils;

import java.util.UUID;

public record EventId(String value) {

    private static final String PREFIX = "evt_";

    public EventId {
        if (StringUtils.isBlank(value)) {
            throw new IllegalArgumentException("Event id must not be blank");
        }
    }

    public static EventId generate() {
        String uuid = UUID.randomUUID()
                .toString()
                .replace("-", "");

        return new EventId(PREFIX + uuid);
    }
}