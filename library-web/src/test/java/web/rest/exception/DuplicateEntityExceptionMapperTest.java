package web.rest.exception;

import common.exception.DuplicateEntityException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DuplicateEntityExceptionMapper.
 *
 * Duplicate-entity violations are reported as 409 Conflict — the request is
 * structurally valid but the submitted data conflicts with an existing record
 * (e.g. registering a book whose ISBN is already on file).
 */
@DisplayName("DuplicateEntityExceptionMapper")
class DuplicateEntityExceptionMapperTest {

    private final DuplicateEntityExceptionMapper mapper = new DuplicateEntityExceptionMapper();

    @Test
    @DisplayName("Should produce a 409 Conflict status")
    void shouldReturn409Status() {
        DuplicateEntityException exception = new DuplicateEntityException("Book", "ISBN", "978-0-13-468599-1");

        Response response = mapper.toResponse(exception);

        assertEquals(409, response.getStatus());
    }

    @Test
    @DisplayName("Should set Content-Type to application/json")
    void shouldSetJsonContentType() {
        DuplicateEntityException exception = new DuplicateEntityException("Book", "ISBN", "978-0-13-468599-1");

        Response response = mapper.toResponse(exception);

        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
    }

    @Test
    @DisplayName("Should produce an ErrorResponse body with correct fields")
    void shouldProduceStructuredErrorBody() {
        DuplicateEntityException exception = new DuplicateEntityException("Book", "ISBN", "978-0-13-468599-1");

        Response response = mapper.toResponse(exception);
        ErrorResponse error = (ErrorResponse) response.getEntity();

        assertAll("ErrorResponse fields",
                () -> assertEquals(409, error.getStatus()),
                () -> assertEquals("Duplicate Entity", error.getError()),
                () -> assertNotNull(error.getMessage(),
                        "Message derived from entity name, field, and value must be present"),
                () -> assertNotNull(error.getTimestamp(), "Timestamp must be set")
        );
    }
}
