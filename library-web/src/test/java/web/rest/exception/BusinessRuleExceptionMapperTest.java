package web.rest.exception;

import common.exception.BusinessRuleException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BusinessRuleExceptionMapper.
 *
 * Business rule violations are reported as 409 Conflict — semantically correct
 * because the request is well-formed but conflicts with the current system state
 * (e.g. deleting a book that has active loans).
 */
@DisplayName("BusinessRuleExceptionMapper")
class BusinessRuleExceptionMapperTest {

    private final BusinessRuleExceptionMapper mapper = new BusinessRuleExceptionMapper();

    @Test
    @DisplayName("Should produce a 409 Conflict status")
    void shouldReturn409Status() {
        BusinessRuleException exception = new BusinessRuleException("Cannot delete book with active loans");

        Response response = mapper.toResponse(exception);

        assertEquals(409, response.getStatus());
    }

    @Test
    @DisplayName("Should set Content-Type to application/json")
    void shouldSetJsonContentType() {
        BusinessRuleException exception = new BusinessRuleException("rule violated");

        Response response = mapper.toResponse(exception);

        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
    }

    @Test
    @DisplayName("Should produce an ErrorResponse body with correct fields")
    void shouldProduceStructuredErrorBody() {
        BusinessRuleException exception = new BusinessRuleException("Cannot delete book with active loans");

        Response response = mapper.toResponse(exception);
        ErrorResponse error = (ErrorResponse) response.getEntity();

        assertAll("ErrorResponse fields",
                () -> assertEquals(409, error.getStatus()),
                () -> assertEquals("Business Rule Violation", error.getError()),
                () -> assertEquals("Cannot delete book with active loans", error.getMessage(),
                        "Original exception message must be preserved verbatim"),
                () -> assertNotNull(error.getTimestamp(), "Timestamp must be set")
        );
    }
}
