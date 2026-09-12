package io.asteria.common.application.exception;

/** Preserves the error returned by a remote service. */
public class RemoteServiceException extends RuntimeException {
    private final String code;

    public RemoteServiceException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
