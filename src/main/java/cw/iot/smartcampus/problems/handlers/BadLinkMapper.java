package cw.iot.smartcampus.problems.handlers;

import cw.iot.smartcampus.models.ApiError;
import cw.iot.smartcampus.problems.LinkedResourceNotFoundException;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

// Maps a bad foreign-key reference inside a request body to HTTP 422.
//
// Why 422 and not 404? The endpoint URI itself (e.g. POST /api/v1/sensors)
// is perfectly valid and *was* found - what is wrong is one of the values
// inside the JSON body the client posted. 422 communicates exactly that:
// "your request was syntactically fine but I cannot process it because
// of a semantic problem with its content".
@Provider
public class BadLinkMapper implements ExceptionMapper<LinkedResourceNotFoundException> {

    // 422 is not in the JAX-RS Status enum, so I use the numeric code
    private static final int UNPROCESSABLE_ENTITY = 422;

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(LinkedResourceNotFoundException ex) {
        String path = (uriInfo != null) ? uriInfo.getPath() : null;

        ApiError body = new ApiError(
                UNPROCESSABLE_ENTITY,
                "Unprocessable Entity",
                ex.getMessage(),
                path
        );

        return Response.status(UNPROCESSABLE_ENTITY)
                       .type(MediaType.APPLICATION_JSON)
                       .entity(body)
                       .build();
    }
}
