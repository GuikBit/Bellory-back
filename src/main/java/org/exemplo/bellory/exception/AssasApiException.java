package org.exemplo.bellory.exception;

import lombok.Getter;

@Getter
public class AssasApiException extends RuntimeException {

    private final int statusCode;
    private final String responseBody;

    public AssasApiException(String message) {
        super(message);
        this.statusCode = 0;
        this.responseBody = null;
    }

    public AssasApiException(String message, int statusCode, String responseBody) {
        super(message);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public AssasApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 0;
        this.responseBody = null;
    }

    public AssasApiException(String message, int statusCode, String responseBody, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }
}
