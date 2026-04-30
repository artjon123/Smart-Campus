package cw.iot.smartcampus.endpoints;

import cw.iot.smartcampus.models.Sensor;
import cw.iot.smartcampus.models.SensorReading;
import cw.iot.smartcampus.problems.SensorUnavailableException;
import cw.iot.smartcampus.store.AppState;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * Sub-resource for /api/v1/sensors/{sensorId}/readings.
 *
 * Note that this class does not have its own @Path annotation - it is
 * mounted by SensorResource#readingsFor() and the parent sensor is
 * passed in through the constructor. That keeps the URL hierarchy clear
 * and means I don't have to look up the sensor again here.
 *
 * Don't add @Provider here either - sub-resources are NOT singletons,
 * they're created on demand for a single request.
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ReadingsLog {

    private final Sensor parent;
    private final AppState store = AppState.get();

    public ReadingsLog(Sensor parent) {
        this.parent = parent;
    }

    /* GET /api/v1/sensors/{sensorId}/readings */
    @GET
    public List<SensorReading> history() {
        return store.readingsOf(parent.getId());
    }

    /* POST /api/v1/sensors/{sensorId}/readings */
    @POST
    public Response submit(SensorReading incoming) {

        // Part 5.3 - reject readings for a sensor that isn't ACTIVE.
        // Strictly the spec only mentions MAINTENANCE, but OFFLINE is
        // logically the same situation: device cannot supply data.
        if (parent.getStatus() == null
            || !Sensor.STATUS_ACTIVE.equalsIgnoreCase(parent.getStatus())) {
            throw new SensorUnavailableException(parent.getId(), parent.getStatus());
        }

        // accept partial bodies - the client only really has to send the
        // value, we'll mint an id + timestamp ourselves so two readings
        // with the same client clock cannot collide
        if (incoming == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity("Reading body must contain at least a 'value'.")
                           .type(MediaType.TEXT_PLAIN)
                           .build();
        }

        SensorReading toStore;
        if (incoming.getId() == null || incoming.getId().isBlank()) {
            toStore = SensorReading.freshReading(incoming.getValue());
        } else {
            // honour the client-supplied id and timestamp where present
            long ts = incoming.getTimestamp() == 0
                    ? System.currentTimeMillis()
                    : incoming.getTimestamp();
            toStore = new SensorReading(incoming.getId(), ts, incoming.getValue());
        }

        // append + side effect (Part 4.2): the store updates currentValue
        // on the parent atomically so subsequent GET /sensors/{id} reflects
        // the latest reading
        store.appendReading(parent.getId(), toStore);

        return Response.status(Response.Status.CREATED).entity(toStore).build();
    }
}
