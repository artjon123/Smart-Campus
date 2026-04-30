package cw.iot.smartcampus.store;

import cw.iot.smartcampus.models.Room;
import cw.iot.smartcampus.models.Sensor;
import cw.iot.smartcampus.models.SensorReading;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory storage for the whole application.
 *
 * I went with a singleton because in JAX-RS every Resource class is
 * instantiated per-request by default. If the maps lived inside the
 * resources they would be wiped between calls. Pulling them out into
 * one shared object that lives for the lifetime of the JVM is the
 * straightforward fix.
 *
 * Concurrency notes:
 *  - rooms / sensors are ConcurrentHashMap so basic put/get/remove
 *    are already thread-safe.
 *  - readings is a map of synchronizedList. Whenever I iterate that
 *    list I wrap the iteration in a synchronized block.
 *  - For multi-step writes (e.g. "remove room AND clear its sensor list")
 *    I lock on the AppState instance itself so two concurrent deletes
 *    cannot half-succeed and leave the data inconsistent.
 *
 * The brief forbids real databases, so this is the substitute.
 */
public final class AppState {

    private static final AppState INSTANCE = new AppState();

    public static AppState get() {
        return INSTANCE;
    }

    // package-private boot hook used from Bootstrap.main so the tests
    // see some realistic data on first start
    public static void bootstrap() {
        INSTANCE.seed();
    }

    private final Map<String, Room>   rooms   = new ConcurrentHashMap<>();
    private final Map<String, Sensor> sensors = new ConcurrentHashMap<>();
    // sensor id -> append-only list of readings for that sensor
    private final Map<String, List<SensorReading>> readings = new ConcurrentHashMap<>();

    private AppState() {
        // private to keep the singleton honest
    }

    /* ---------- rooms ---------- */

    public Collection<Room> allRooms() {
        return rooms.values();
    }

    public Room findRoom(String id) {
        return rooms.get(id);
    }

    public boolean hasRoom(String id) {
        return rooms.containsKey(id);
    }

    public void saveRoom(Room r) {
        rooms.put(r.getId(), r);
    }

    /**
     * Removes the room and returns true on success, false if the room is
     * unknown. Throws nothing - the resource layer handles "room had
     * sensors" using a custom exception before calling here.
     */
    public synchronized boolean dropRoom(String id) {
        return rooms.remove(id) != null;
    }

    /* ---------- sensors ---------- */

    public Collection<Sensor> allSensors() {
        return sensors.values();
    }

    /** Filtered version used by GET /sensors?type=... */
    public List<Sensor> sensorsOfType(String type) {
        List<Sensor> out = new ArrayList<>();
        for (Sensor s : sensors.values()) {
            if (s.getType() != null && s.getType().equalsIgnoreCase(type)) {
                out.add(s);
            }
        }
        return out;
    }

    public Sensor findSensor(String id) {
        return sensors.get(id);
    }

    public boolean hasSensor(String id) {
        return sensors.containsKey(id);
    }

    /**
     * Persists the sensor and ALSO links it to its parent room so that
     * Room.getSensorIds() stays in sync. Callers must already have
     * validated the room exists - this is enforced in the resource layer
     * which throws LinkedResourceNotFoundException before we get here.
     */
    public synchronized void saveSensor(Sensor s) {
        sensors.put(s.getId(), s);
        Room parent = rooms.get(s.getRoomId());
        if (parent != null) {
            parent.attachSensor(s.getId());
        }
        // make sure there is a readings bucket ready for it
        readings.computeIfAbsent(s.getId(),
                                 k -> Collections.synchronizedList(new ArrayList<>()));
    }

    /* ---------- readings ---------- */

    public List<SensorReading> readingsOf(String sensorId) {
        List<SensorReading> list = readings.get(sensorId);
        if (list == null) {
            return Collections.emptyList();
        }
        // hand back a defensive copy so the resource layer can iterate
        // it without holding the synchronizedList monitor
        synchronized (list) {
            return new ArrayList<>(list);
        }
    }

    /**
     * Append a new reading and update the parent sensor's currentValue
     * so the resource representations stay coherent. Both operations are
     * done under one synchronized block so a concurrent reader never
     * observes "value updated but reading missing" or vice versa.
     */
    public void appendReading(String sensorId, SensorReading r) {
        Sensor s = sensors.get(sensorId);
        if (s == null) {
            // shouldn't happen if the resource validated up front, but
            // I guard anyway so a programming mistake doesn't corrupt state
            return;
        }
        List<SensorReading> bucket =
                readings.computeIfAbsent(sensorId,
                                         k -> Collections.synchronizedList(new ArrayList<>()));
        synchronized (bucket) {
            bucket.add(r);
            s.setCurrentValue(r.getValue());
        }
    }

    /* ---------- seed data so the API isn't empty on first boot ---------- */

    private void seed() {
        if (!rooms.isEmpty()) {
            return; // safety - bootstrap might be called twice in tests
        }

        Room library = new Room("LIB-301", "Library Quiet Study", 40);
        Room lecture = new Room("LEC-110", "Lecture Hall A", 220);
        Room lab     = new Room("LAB-205", "Networking Lab",   30);
        saveRoom(library);
        saveRoom(lecture);
        saveRoom(lab);

        Sensor temp1 = new Sensor("TEMP-001", "Temperature", Sensor.STATUS_ACTIVE,
                                  21.4, library.getId());
        Sensor co2_1 = new Sensor("CO2-007",  "CO2",         Sensor.STATUS_ACTIVE,
                                  640.0, lecture.getId());
        Sensor occ1  = new Sensor("OCC-014",  "Occupancy",   Sensor.STATUS_MAINTENANCE,
                                  0.0, lab.getId());

        saveSensor(temp1);
        saveSensor(co2_1);
        saveSensor(occ1);

        // a couple of readings so GET /sensors/TEMP-001/readings returns
        // something meaningful out of the box
        appendReading(temp1.getId(), new SensorReading(
                "seed-r1", System.currentTimeMillis() - 60_000, 21.1));
        appendReading(temp1.getId(), new SensorReading(
                "seed-r2", System.currentTimeMillis() - 30_000, 21.4));
    }
}
