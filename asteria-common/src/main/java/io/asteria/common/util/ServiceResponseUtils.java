package io.asteria.common.util;

import io.asteria.common.application.exception.RemoteServiceException;
import io.asteria.common.application.response.ServiceResponse;

public final class ServiceResponseUtils {

    private ServiceResponseUtils() {
    }

    /** Returns successful data (including null), or throws the remote error. */
    public static <T> T getData(ServiceResponse<T> response) {
        if (response == null) {
            throw new RemoteServiceException("REMOTE_RESPONSE_EMPTY", "Remote service returned no response");
        }
        if (!response.success()) {
            throw new RemoteServiceException(response.code(), response.message());
        }
        return response.data();
    }
}
