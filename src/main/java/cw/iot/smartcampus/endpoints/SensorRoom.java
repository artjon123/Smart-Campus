package cw.iot.smartcampus.endpoints;

import cw.iot.smartcampus.models.Room;
import cw.iot.smartcampus.problems.RoomNotEmptyException;
import cw.iot.smartcampus.store.AppState;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Collection;

// Manages the /api/v1/rooms collection.
// Class name "SensorRoom" comes straight from the brief - I stuck with
// it even though "RoomEndpoint" would have read more naturally, because
// changing class names against the spec felt risky.
@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorRoom {

    private final AppState store = AppState.get();

    /* GET /api/v1/rooms - list every room. */
    @GET
    public Collection<Room> listAll() {
        return store.allRooms();
    }

    /* POST /api/v1/rooms - create a new room. */
    @POST
    public Response create(Room incoming, @Context UriInfo uri) {
        // very small bit of validation - if the client forgot to send
        // an id, reject the request. Better than letting the map
        // silently key on null.
        if (incoming.getId() == null || incoming.getId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity("Field 'id' is required to create a room.")
                           .type(MediaType.TEXT_PLAIN)
                           .build();
        }

        store.saveRoom(incoming);

        // 201 Created with a Location header that points at the new
        // resource. We build off UriInfo so the result includes the
        // application path prefix (/api/v1) - UriBuilder.fromResource
        // would lose it under Grizzly.
        URI location = uri.getBaseUriBuilder()
                          .path("rooms")
                          .path(incoming.getId())
                          .build();
        return Response.created(location).entity(incoming).build();
    }

    /* GET /api/v1/rooms/{roomId} - fetch one room by id. */
    @GET
    @Path("/{roomId}")
    public Room getOne(@PathParam("roomId") String roomId) {
        Room r = store.findRoom(roomId);
        if (r == null) {
            // bare JAX-RS exception, the catch-all mapper will format it
            throw new NotFoundException("No room with id " + roomId);
        }
        return r;
    }

    /* DELETE /api/v1/rooms/{roomId} - decommission a room. */
    @DELETE
    @Path("/{roomId}")
    public Response delete(@PathParam("roomId") String roomId) {
        Room r = store.findRoom(roomId);
        if (r == null) {
            // already gone - the question in the brief specifically asks
            // about idempotency; returning 404 here is consistent with
            // "already deleted, nothing to do".
            return Response.status(Response.Status.NOT_FOUND)
                           .entity("Room " + roomId + " not found.")
                           .type(MediaType.TEXT_PLAIN)
                           .build();
        }

        // safety logic from Part 2.2: we refuse to delete a room with
        // sensors still attached, otherwise we'd orphan the sensors.
        int attached = r.getSensorIds().size();
        if (attached > 0) {
            throw new RoomNotEmptyException(roomId, attached);
        }

        store.dropRoom(roomId);
        // 204 No Content - the body would just be empty anyway
        return Response.noContent().build();
    }
}
