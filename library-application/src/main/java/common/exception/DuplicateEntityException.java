package common.exception;

import java.io.Serial;

public class DuplicateEntityException extends ApplicationException{

    @Serial
    private static final long serialVersionUID = 1L;

    public DuplicateEntityException(String message) {
        super(message);
    }

    public DuplicateEntityException(ExceptionMessage exceptionMessage) {
        super(exceptionMessage);
    }

    public DuplicateEntityException(ExceptionMessage exceptionMessage, Object... params) {
        super(exceptionMessage, params);
    }

    public DuplicateEntityException(String entityName, String field, Object value) {
        super(ExceptionMessage.ENTITY_DUPLICATE, entityName, field, value);
    }
}
