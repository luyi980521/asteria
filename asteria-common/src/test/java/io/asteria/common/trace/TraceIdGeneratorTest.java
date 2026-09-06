package io.asteria.common.trace;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TraceIdGeneratorTest {
    @Test
    void generatesDistinct32CharacterIds() {
        String first = TraceIdGenerator.generate();
        assertTrue(first.matches("[0-9a-f]{32}"));
        assertNotEquals(first, TraceIdGenerator.generate());
        assertEquals("request-1", TraceIdGenerator.resolve("request-1"));
        assertTrue(TraceIdGenerator.resolve(" ").matches("[0-9a-f]{32}"));
    }
}
