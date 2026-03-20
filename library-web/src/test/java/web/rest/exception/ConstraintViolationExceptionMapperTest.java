package web.rest.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ConstraintViolationExceptionMapper.
 *
 * Bean Validation violations are reported as 400 Bad Request. The mapper joins
 * all violation messages into a single string, formatted as
 * "propertyPath: message; propertyPath: message".
 *
 * ConstraintViolation objects cannot be instantiated directly (they are produced
 * by the validator), so we mock them to control the property path and message.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ConstraintViolationExceptionMapper")
class ConstraintViolationExceptionMapperTest {

    private final ConstraintViolationExceptionMapper mapper = new ConstraintViolationExceptionMapper();

    @Mock
    private Path violationPath;

    @Test
    @DisplayName("Should produce a 400 Bad Request status")
    @SuppressWarnings("unchecked")
    void shouldReturn400Status() {
        ConstraintViolation<?> violation = buildViolation("isbn", "must not be blank");
        ConstraintViolationException exception = new ConstraintViolationException(Set.of(violation));

        Response response = mapper.toResponse(exception);

        assertEquals(400, response.getStatus());
    }

    @Test
    @DisplayName("Should set Content-Type to application/json")
    @SuppressWarnings("unchecked")
    void shouldSetJsonContentType() {
        ConstraintViolation<?> violation = buildViolation("isbn", "must not be blank");
        ConstraintViolationException exception = new ConstraintViolationException(Set.of(violation));

        Response response = mapper.toResponse(exception);

        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
    }

    @Test
    @DisplayName("Should produce an ErrorResponse with joined violation messages")
    @SuppressWarnings("unchecked")
    void shouldProduceStructuredErrorBody() {
        ConstraintViolation<?> violation = buildViolation("isbn", "must not be blank");
        ConstraintViolationException exception = new ConstraintViolationException(Set.of(violation));

        Response response = mapper.toResponse(exception);
        ErrorResponse error = (ErrorResponse) response.getEntity();

        assertAll("ErrorResponse fields",
                () -> assertEquals(400, error.getStatus()),
                () -> assertEquals("Validation Error", error.getError()),
                () -> assertTrue(error.getMessage().contains("isbn: must not be blank"),
                        "Message must include 'propertyPath: message' for each violation"),
                () -> assertNotNull(error.getTimestamp(), "Timestamp must be set")
        );
    }

    /**
     * Creates a mocked ConstraintViolation with the given property path and message.
     * Both ConstraintViolation and Path are interfaces produced by the validation
     * framework — mocking is the only viable approach in a pure unit test.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private ConstraintViolation<?> buildViolation(String propertyPathStr, String message) {
        when(violationPath.toString()).thenReturn(propertyPathStr);

        ConstraintViolation violation = org.mockito.Mockito.mock(ConstraintViolation.class);
        when(violation.getPropertyPath()).thenReturn(violationPath);
        when(violation.getMessage()).thenReturn(message);
        return violation;
    }
}
