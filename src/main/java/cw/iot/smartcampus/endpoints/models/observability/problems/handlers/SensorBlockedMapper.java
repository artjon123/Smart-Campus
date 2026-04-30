package cw.iot.smartcampus.problems.handlers;

import cw.iot.smartcampus.models.ApiError;
import cw.iot.smartcampus.problems.SensorUnavailableException;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

// Maps SensorUnavailableException -> 403 Forbidden as required by part 5.3.
//
// I read the brief twice on this one: 403 normally means "the caller is
// not allowed", which feels off. But the spec is clear, and the
// justification holds - the API is refusing the action because of the
// resource state, regardless of who the caller is. Treat it as a
// state-based prohibition rather than an authentication thing.
@Provider
public class SensorBlockedMapper implements ExceptionMapper<SensorUnavailableException> {

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(SensorUnavailableException ex) {
        String path = (uriInfo != null) ? uriInfo.getPath() : null;

        ApiError body = new ApiError(
                Response.Status.FORBIDDEN.getStatusCode(),
                "Forbidden",
                ex.getMessage(),
                path
        );

        return Response.status(Response.Status.FORBIDDEN)
                       .type(MediaType.APPLICATION_JSON)
                       .entity(body)
                       .build();
    }
}
