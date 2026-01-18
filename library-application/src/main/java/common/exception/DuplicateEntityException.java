package common.exception;

import java.io.Serial;

public class DuplicateEntityException extends ApplicationException{

    @Serial
    private static final long serialVersionUID = 1L;

    public DuplicateEntityException(String entityName, String field, String value) {
        super(entityName + " with " + field + " '" + value + "' already exists");
    }

    public DuplicateEntityException(String message) {
        super(message);
    }
}
