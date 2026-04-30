package cw.iot.smartcampus.endpoints;

import cw.iot.smartcampus.models.Sensor;
import cw.iot.smartcampus.problems.LinkedResourceNotFoundException;
import cw.iot.smartcampus.store.AppState;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Collection;

// Manages /api/v1/sensors and acts as the entry point to the readings
// sub-resource. The sub-resource locator at the bottom is the
// interesting bit for Part 4.
@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    private final AppState store = AppState.get();

    /* GET /api/v1/sensors  (optional ?type=CO2) */
    @GET
    public Collection<Sensor> list(@QueryParam("type") String typeFilter) {
        if (typeFilter == null || typeFilter.isBlank()) {
            return store.allSensors();
        }
        // Filtering on type is exactly the use case @QueryParam was made
        // for - it's optional, has no impact on the resource identity,
        // and naturally composes with future filters like ?status=...
        return store.sensorsOfType(typeFilter);
    }

    /* POST /api/v1/sensors - register a new device. */
    @POST
    public Response register(Sensor incoming, @Context UriInfo uri) {
        if (incoming.getId() == null || incoming.getId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity("Field 'id' is required to create a sensor.")
                           .type(MediaType.TEXT_PLAIN)
                           .build();
        }

        // Part 3.1 integrity check: the roomId in the body must point
        // at an existing room, otherwise we'd be creating an orphan.
        // We use a 422 rather than a 404 here (see BadLinkMapper).
        if (incoming.getRoomId() == null || !store.hasRoom(incoming.getRoomId())) {
            throw new LinkedResourceNotFoundException(
                    "roomId",
                    String.valueOf(incoming.getRoomId()));
        }

        // sensible default if the client omits status - assume ACTIVE
        if (incoming.getStatus() == null || incoming.getStatus().isBlank()) {
            incoming.setStatus(Sensor.STATUS_ACTIVE);
        }

        store.saveSensor(incoming);

        // build the absolute URI from UriInfo so /api/v1 is included
        URI location = uri.getBaseUriBuilder()
                          .path("sensors")
                          .path(incoming.getId())
                          .build();
        return Response.created(location).entity(incoming).build();
    }

    /* GET /api/v1/sensors/{sensorId} - lookup a single sensor. */
    @GET
    @Path("/{sensorId}")
    public Sensor lookup(@PathParam("sensorId") String sensorId) {
        Sensor s = store.findSensor(sensorId);
        if (s == null) {
            throw new NotFoundException("No sensor with id " + sensorId);
        }
        return s;
    }

    /**
     * Sub-resource locator (Part 4.1).
     *
     * Rather than putting the readings methods on this class, we delegate
     * everything underneath /sensors/{id}/readings to a separate
     * ReadingsLog instance. The advantages over a flat controller are:
     *   - this class stays small and focused on sensor CRUD;
     *   - the sub-resource gets a handle on the parent sensor it cares
     *     about, so its own methods don't need to re-look it up;
     *   - if we add e.g. /sensors/{id}/alerts later it can become
     *     another sibling sub-resource without us bloating SensorResource.
     */
    @Path("/{sensorId}/readings")
    public ReadingsLog readingsFor(@PathParam("sensorId") String sensorId) {
        Sensor parent = store.findSensor(sensorId);
        if (parent == null) {
            throw new NotFoundException("No sensor with id " + sensorId);
        }
        return new ReadingsLog(parent);
    }
}
