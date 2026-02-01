package org.exemplo.bellory.model.entity.questionario.exception;

public class QuestionarioException extends RuntimeException {

    private final int errorCode;

    public QuestionarioException(String message) {
        super(message);
        this.errorCode = 400;
    }

    public QuestionarioException(String message, int errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public QuestionarioException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = 500;
    }

    public QuestionarioException(String message, int errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return errorCode;
    }
}
