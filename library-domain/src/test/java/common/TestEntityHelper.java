package common;

import java.lang.reflect.Field;

/**
 * Utility class for setting private fields on BaseEntity during unit tests.
 *
 * BaseEntity's id, version, createdAt, and updatedAt are managed by JPA
 * and have no public setters. In unit tests (without a persistence context),
 * we need reflection to simulate persisted entities with assigned IDs.
 *
 * Usage:
 *   Book book = new Book("isbn", "title", "author");
 *   common.TestEntityHelper.setId(book, 1L);
 */
public final class TestEntityHelper {

    private TestEntityHelper() {
        // Utility class — prevent instantiation
    }

    /**
     * Sets the 'id' field on a BaseEntity subclass.
     * This simulates a persisted entity that JPA would normally manage.
     */
    public static void setId(BaseEntity entity, Long id) {
        setField(entity, "id", id);
    }

    /**
     * Sets the 'version' field on a BaseEntity subclass.
     */
    public static void setVersion(BaseEntity entity, Long version) {
        setField(entity, "version", version);
    }

    /**
     * Generic field setter using reflection.
     * Walks up the class hierarchy to find the declared field.
     */
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
     * Necessary because 'id' lives in BaseEntity, not in the concrete subclass.
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
