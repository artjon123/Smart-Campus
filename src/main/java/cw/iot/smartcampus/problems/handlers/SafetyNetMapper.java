package cw.iot.smartcampus.problems.handlers;

import cw.iot.smartcampus.models.ApiError;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.logging.Level;
import java.util.logging.Logger;

// The "global safety net" required by part 5.4. Anything that is not a
// JAX-RS WebApplicationException and not handled by one of the more
// specific mappers above lands here. The client only ever sees a
// generic 500 with no internal detail; the real exception is logged
// on the server side so I (the developer) can still debug it.
//
// Important: this mapper deliberately does not call ex.getMessage()
// in the response body. Some unexpected exceptions like NullPointerException
// have messages that quote internal class names or even bits of input -
// returning that to the caller would defeat the whole point of the
// safety net.
@Provider
public class SafetyNetMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(SafetyNetMapper.class.getName());

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(Throwable ex) {

        // If the exception is already a JAX-RS one (e.g. NotFoundException
        // when a route doesn't match) we should respect the status it
        // wants. Otherwise we'd convert every 404 into a 500 which would
        // be very wrong.
        if (ex instanceof WebApplicationException) {
            WebApplicationException wae = (WebApplicationException) ex;
            Response existing = wae.getResponse();
            int code = existing.getStatus();
            String reason = Response.Status.fromStatusCode(code) != null
                          ? Response.Status.fromStatusCode(code).getReasonPhrase()
                          : "Error";
            ApiError body = new ApiError(
                    code,
                    reason,
                    safeMsg(wae, "Request could not be completed."),
                    uriInfo != null ? uriInfo.getPath() : null);
            return Response.status(code)
                           .type(MediaType.APPLICATION_JSON)
                           .entity(body)
                           .build();
        }

        // genuinely unexpected - log loudly, hide the details from the client
        LOG.log(Level.SEVERE, "Unhandled exception while processing request", ex);

        ApiError body = new ApiError(
                500,
                "Internal Server Error",
                "Something went wrong on our side. The incident has been logged.",
                uriInfo != null ? uriInfo.getPath() : null);

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                       .type(MediaType.APPLICATION_JSON)
                       .entity(body)
                       .build();
    }

    private static String safeMsg(Throwable t, String fallback) {
        String m = t.getMessage();
        return (m == null || m.isBlank()) ? fallback : m;
    }
}
