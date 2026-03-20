package common;

import jakarta.validation.Validator;

import java.lang.reflect.Field;

/**
 * Utility class for injecting test infrastructure into service instances.
 *
 * Services extend BaseService, which declares a CDI-managed Validator with no
 * public setter. Mockito's constructor injection (used when @InjectMocks targets
 * a class with a multi-arg constructor) does not subsequently perform field
 * injection on superclass fields, so we use reflection to fill that gap.
 *
 * Also provides setEntityId() for simulating persisted entities in service-layer
 * tests, mirroring the TestEntityHelper pattern established in domain tests.
 *
 * Usage:
 *   @BeforeEach
 *   void setUp() {
 *       TestServiceHelper.injectValidator(bookService, validator);
 *   }
 */
public final class TestServiceHelper {

    private TestServiceHelper() {
        // Utility class — prevent instantiation
    }

    /**
     * Injects a Validator mock into BaseService.validator.
     *
     * Call this in @BeforeEach after Mockito's @InjectMocks has created the service.
     * Without this, validateFieldsConstraint() will throw NullPointerException
     * in any test that exercises create(), update(), or markAsLost() paths.
     */
    public static void injectValidator(BaseService<?> service, Validator validator) {
        setField(service, "validator", validator);
    }

    /**
     * Sets the 'id' field on any object that carries a BaseEntity.id field.
     *
     * Needed when tests require persisted-entity semantics: ID comparisons in
     * duplicate-detection logic (e.g. "does this ISBN belong to a different book?")
     * depend on getId() returning a non-null value.
     */
    public static void setEntityId(Object entity, Long id) {
        setField(entity, "id", id);
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = findField(target.getClass(), fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(
                    "Failed to set field '" + fieldName + "' on " + target.getClass().getSimpleName(), e);
        }
    }

    /**
     * Traverses the class hierarchy to locate a declared field.
     * Necessary because 'id' lives in BaseEntity and 'validator' lives in BaseService,
     * not in the concrete subclass being tested.
     */
    private static Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(
                "Field '" + fieldName + "' not found in class hierarchy of " + clazz.getSimpleName());
    }
}
