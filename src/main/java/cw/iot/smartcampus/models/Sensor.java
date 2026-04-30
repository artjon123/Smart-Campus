package cw.iot.smartcampus.models;

// Sensor POJO. The spec lists three valid status values - "ACTIVE",
// "MAINTENANCE", "OFFLINE". I am keeping it as a plain String rather
// than an enum so that JSON parsing stays simple and so my exception
// mapper can quote whatever the client sent if it is wrong.
public class Sensor {

    public static final String STATUS_ACTIVE       = "ACTIVE";
    public static final String STATUS_MAINTENANCE  = "MAINTENANCE";
    public static final String STATUS_OFFLINE      = "OFFLINE";

    private String id;             // e.g. "TEMP-001"
    private String type;           // "Temperature", "CO2", "Occupancy" ...
    private String status;         // ACTIVE / MAINTENANCE / OFFLINE
    private double currentValue;   // most recent reading the device sent us
    private String roomId;         // foreign key to Room.id

    public Sensor() { }

    public Sensor(String id, String type, String status, double currentValue, String roomId) {
        this.id = id;
        this.type = type;
        this.status = status;
        this.currentValue = currentValue;
        this.roomId = roomId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getCurrentValue() { return currentValue; }
    public void setCurrentValue(double currentValue) { this.currentValue = currentValue; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
}
