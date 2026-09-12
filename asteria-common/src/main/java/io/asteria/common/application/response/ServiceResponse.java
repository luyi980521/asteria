package io.asteria.common.application.response;

/** Common response contract independent of the HTTP transport. */
public interface ServiceResponse<T> {
    boolean success();
    String code();
    String message();
    T data();
}
