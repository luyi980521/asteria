package io.asteria.web.response;

import io.asteria.common.util.JsonUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies factories and the exact public JSON envelope.
 */
class ApiResponseTest {
    @Test
    void successContainsData() {
        ApiResponse<Long> response = ApiResponse.success(1001L);
        assertTrue(response.success());
        assertEquals("SUCCESS", response.code());
        assertEquals("Success", response.message());
        assertEquals(1001L, response.data());
        assertEquals(JsonUtils.readTree("""
                {"success":true,"code":"SUCCESS","message":"Success","data":1001}
                """), JsonUtils.readTree(JsonUtils.toJson(response)));
    }

    @Test
    void successWithoutDataRetainsExplicitNull() {
        ApiResponse<Void> response = ApiResponse.successWithoutData();
        assertTrue(response.success());
        assertNull(response.data());
        assertEquals(JsonUtils.readTree("""
                {"success":true,"code":"SUCCESS","message":"Success","data":null}
                """), JsonUtils.readTree(JsonUtils.toJson(response)));
    }

    @Test
    void failurePreservesCodeAndMessageWithNullData() {
        ApiResponse<Void> response = ApiResponse.failure("LEDGER_0030", "Ledger account is already exists");
        assertFalse(response.success());
        assertEquals("LEDGER_0030", response.code());
        assertEquals("Ledger account is already exists", response.message());
        assertNull(response.data());
        assertEquals(JsonUtils.readTree("""
                {"success":false,"code":"LEDGER_0030","message":"Ledger account is already exists","data":null}
                """), JsonUtils.readTree(JsonUtils.toJson(response)));
    }
}
