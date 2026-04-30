package cw.iot.smartcampus.models;

import java.util.ArrayList;
import java.util.List;

// Room POJO - one of the three required entities in the spec.
// Each Room has zero or more sensors deployed inside it. The relationship
// is captured by storing sensor IDs (not full objects) so we don't end up
// in an infinite serialisation loop when Jackson tries to expand them.
public class Room {

    private String id;                // e.g. "LIB-301"
    private String name;              // e.g. "Library Quiet Study"
    private int capacity;             // max occupancy for safety regs
    private List<String> sensorIds = new ArrayList<>();

    // Jackson needs the no-arg constructor when deserialising POSTs
    public Room() { }

    public Room(String id, String name, int capacity) {
        this.id = id;
        this.name = name;
        this.capacity = capacity;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public List<String> getSensorIds() { return sensorIds; }
    public void setSensorIds(List<String> sensorIds) {
        // never store null - keeps the JSON output clean and the helper
        // methods below from blowing up
        this.sensorIds = (sensorIds == null) ? new ArrayList<>() : sensorIds;
    }

    // tiny helper used by the store when a sensor is added/removed.
    // I put it here rather than in the store so the Room owns its own
    // list invariants.
    public void attachSensor(String sensorId) {
        if (!sensorIds.contains(sensorId)) {
            sensorIds.add(sensorId);
        }
    }

    public void detachSensor(String sensorId) {
        sensorIds.remove(sensorId);
    }
}
