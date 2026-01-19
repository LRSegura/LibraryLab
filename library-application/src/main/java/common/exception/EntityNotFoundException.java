package common.exception;

import java.io.Serial;

public class EntityNotFoundException extends ApplicationException {

    @Serial
    private static final long serialVersionUID = 1L;

    public EntityNotFoundException(String entityName, Long id) {
        super(entityName + " not found with id: " + id);
    }

    public EntityNotFoundException(String entityName, String field, String value) {
        super(entityName + " not found with " + field + ": " + value);
    }

    public EntityNotFoundException(String message) {
        super(message);
    }

    public EntityNotFoundException(ExceptionMessage exceptionMessage) {
        super(exceptionMessage);
    }

    public EntityNotFoundException(ExceptionMessage exceptionMessage, Object... params) {
        super(exceptionMessage, params);
    }

    public EntityNotFoundException(String entityName, String field, Object value) {
        super(ExceptionMessage.ENTITY_NOT_FOUND, entityName, field, value);
    }
}
