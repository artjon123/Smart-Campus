package cw.iot.smartcampus.models;

import java.util.UUID;

// One observation captured by a sensor at a moment in time. The spec
// recommends UUID for the id, so I default to that in the convenience
// factory below. Timestamp is epoch millis (long) - easier to compare
// than a formatted string and the client can format it however they want.
public class SensorReading {

    private String id;
    private long timestamp;
    private double value;

    public SensorReading() { }

    public SensorReading(String id, long timestamp, double value) {
        this.id = id;
        this.timestamp = timestamp;
        this.value = value;
    }

    // factory used by POST /sensors/{id}/readings when the client did not
    // bother to send an id. Generating it server-side guarantees uniqueness
    // and means the client cannot collide two readings on purpose.
    public static SensorReading freshReading(double value) {
        return new SensorReading(UUID.randomUUID().toString(),
                                 System.currentTimeMillis(),
                                 value);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }
}
