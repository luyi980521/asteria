package io.asteria.web.filter;

import io.asteria.common.trace.TraceConstants;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class TraceIdFilterTest {
    private final TraceIdFilter filter = new TraceIdFilter();

    @AfterEach
    void clear() {
        MDC.clear();
    }

    @Test
    void generatesIdAndReturnsItInResponse() throws Exception {
        var response = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest(), response, (request, result) -> {
            assertTrue(MDC.get(TraceConstants.TRACE_ID).matches("[0-9a-f]{32}"));
            assertEquals(MDC.get(TraceConstants.TRACE_ID), response.getHeader(TraceConstants.TRACE_ID_HEADER));
        });
        assertNotNull(response.getHeader(TraceConstants.TRACE_ID_HEADER));
        assertNull(MDC.get(TraceConstants.TRACE_ID));
    }

    @Test
    void preservesSuppliedIdAndOtherMdcKeys() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader(TraceConstants.TRACE_ID_HEADER, "request-A");
        var response = new MockHttpServletResponse();
        MDC.put("other", "kept");
        filter.doFilter(request, response, (req, res) -> assertEquals("request-A", MDC.get(TraceConstants.TRACE_ID)));
        assertEquals("request-A", response.getHeader(TraceConstants.TRACE_ID_HEADER));
        assertEquals("kept", MDC.get("other"));
        assertNull(MDC.get(TraceConstants.TRACE_ID));
    }

    @Test
    void cleansMdcOnFailureAndReplacesBlankHeader() {
        var request = new MockHttpServletRequest();
        request.addHeader(TraceConstants.TRACE_ID_HEADER, " ");
        var response = new MockHttpServletResponse();
        assertThrows(ServletException.class, () -> filter.doFilter(request, response, (req, res) -> {
            assertTrue(MDC.get(TraceConstants.TRACE_ID).matches("[0-9a-f]{32}"));
            throw new ServletException("failure");
        }));
        assertNotNull(response.getHeader(TraceConstants.TRACE_ID_HEADER));
        assertNull(MDC.get(TraceConstants.TRACE_ID));
    }
}
