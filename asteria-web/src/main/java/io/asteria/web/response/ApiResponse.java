package io.asteria.web.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.asteria.common.application.response.ServiceResponse;
import lombok.Builder;

/**
 * Common REST response envelope, including an explicit null data field when empty.
 */
@Builder
@JsonInclude(JsonInclude.Include.ALWAYS)
public record ApiResponse<T>(boolean success, String code, String message, T data) implements ServiceResponse<T> {

    /**
     * Creates a successful response containing the existing endpoint payload.
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .code("SUCCESS")
                .message("Success")
                .data(data)
                .build();
    }

    /**
     * Creates an empty success response; success() is the record's boolean accessor.
     */
    public static ApiResponse<Void> successWithoutData() {
        return ApiResponse.<Void>success(null);
    }

    /**
     * Creates a failure response without exposing a data payload.
     */
    public static <T> ApiResponse<T> failure(String code, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .code(code)
                .message(message)
                .build();
    }
}
