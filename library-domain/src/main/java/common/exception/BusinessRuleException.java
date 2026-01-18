package common.exception;

import java.io.Serial;

public class BusinessRuleException extends ApplicationException {

    @Serial
    private static final long serialVersionUID = 1L;

    public BusinessRuleException(String message) {
        super(message);
    }
}
