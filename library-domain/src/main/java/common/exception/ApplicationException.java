package common.exception;

import java.io.Serial;

public class ApplicationException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ApplicationException(String message) {
        super(message);
    }

    public ApplicationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ApplicationException(ExceptionMessage exceptionMessage) {
        super(exceptionMessage.getMessage());
    }

    public ApplicationException(ExceptionMessage exceptionMessage, Object... params) {
        super(exceptionMessage.getMessage(params));
    }

    public ApplicationException(ExceptionMessage exceptionMessage, Throwable cause, Object... params) {
        super(exceptionMessage.getMessage(params), cause);
    }
}