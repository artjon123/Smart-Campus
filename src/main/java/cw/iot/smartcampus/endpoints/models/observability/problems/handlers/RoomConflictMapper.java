package cw.iot.smartcampus.problems.handlers;

import cw.iot.smartcampus.models.ApiError;
import cw.iot.smartcampus.problems.RoomNotEmptyException;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

// Catches RoomNotEmptyException and turns it into 409 Conflict with our
// shared ApiError JSON payload. 409 is the right code here because the
// resource state (the room having sensors) blocks the requested action;
// the client could retry once the underlying conflict goes away.
@Provider
public class RoomConflictMapper implements ExceptionMapper<RoomNotEmptyException> {

    // @Context lets Jersey inject the request URI so we can report it back
    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(RoomNotEmptyException ex) {
        String path = (uriInfo != null) ? uriInfo.getPath() : null;

        ApiError body = new ApiError(
                Response.Status.CONFLICT.getStatusCode(),
                "Conflict",
                ex.getMessage(),
                path
        );

        return Response.status(Response.Status.CONFLICT)
                       .type(MediaType.APPLICATION_JSON)
                       .entity(body)
                       .build();
    }
}
