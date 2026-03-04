package web.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import web.rest.dto.ApiResponse;

@Path("/application/state")
@Produces(MediaType.APPLICATION_JSON)
public class ApplicationState {

    @GET
    public ApiResponse<String> getApplicationState() {
        return new ApiResponse<>("Application is running");
    }
}
