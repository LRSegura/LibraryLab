package web.rest.exception;

import common.exception.EntityNotFoundException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EntityNotFoundExceptionMapper.
 *
 * These tests verify that the mapper produces the correct HTTP 404 response
 * and that the ErrorResponse body is fully populated. No mocking or container
 * is needed — the mapper is a plain Java class.
 */
@DisplayName("EntityNotFoundExceptionMapper")
class EntityNotFoundExceptionMapperTest {

    private final EntityNotFoundExceptionMapper mapper = new EntityNotFoundExceptionMapper();

    @Test
    @DisplayName("Should produce a 404 Not Found status")
    void shouldReturn404Status() {
        EntityNotFoundException exception = new EntityNotFoundException("Book", "Id", 1L);

        Response response = mapper.toResponse(exception);

        assertEquals(404, response.getStatus());
    }

    @Test
    @DisplayName("Should set Content-Type to application/json")
    void shouldSetJsonContentType() {
        EntityNotFoundException exception = new EntityNotFoundException("Book", "Id", 1L);

        Response response = mapper.toResponse(exception);

        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
    }

    @Test
    @DisplayName("Should produce an ErrorResponse body with correct fields")
    void shouldProduceStructuredErrorBody() {
        EntityNotFoundException exception = new EntityNotFoundException("Book not found with Id: 42");

        Response response = mapper.toResponse(exception);
        ErrorResponse error = (ErrorResponse) response.getEntity();

        assertAll("ErrorResponse fields",
                () -> assertEquals(404, error.getStatus()),
                () -> assertEquals("Not Found", error.getError()),
                () -> assertEquals("Book not found with Id: 42", error.getMessage(),
                        "Original exception message must be preserved verbatim"),
                () -> assertNotNull(error.getTimestamp(), "Timestamp must be set")
        );
    }
}
