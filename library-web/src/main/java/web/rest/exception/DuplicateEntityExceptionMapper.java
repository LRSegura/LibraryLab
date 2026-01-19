package web.rest.exception;

import common.exception.DuplicateEntityException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class DuplicateEntityExceptionMapper implements ExceptionMapper<DuplicateEntityException> {

    @Override
    public Response toResponse(DuplicateEntityException exception) {
        ErrorResponse error = ErrorResponse.of(
                Response.Status.CONFLICT.getStatusCode(),
                "Duplicate Entity",
                exception.getMessage()
        );
        return Response.status(Response.Status.CONFLICT)
                .entity(error)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
