package common;

import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class BaseService <T extends BaseEntity> {

    private final Logger logger = Logger.getLogger(BaseService.class.getName());

    @Inject
    private Validator validator;

    protected void validateFieldsConstraint(T entity) {
        Set<ConstraintViolation<T>> violations = validator.validate(entity);
        if (!violations.isEmpty()) {
            violations.forEach(constraintViolation -> {
                String message = String.format("Validation error - %s: %s", constraintViolation.getPropertyPath(), constraintViolation.getMessage());
                logger.log(Level.SEVERE, message);
            });
            throw new ConstraintViolationException(violations);
        }
    }
}
