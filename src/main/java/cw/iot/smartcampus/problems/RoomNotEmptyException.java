package cw.iot.smartcampus.problems;

// Thrown when a client asks to DELETE a Room that still has sensors
// attached. The resource layer raises this rather than returning a
// Response directly, because then the same logic can be reused (e.g.
// from a cleanup script later) and the mapper handles the HTTP shape.
public class RoomNotEmptyException extends RuntimeException {

    private final String roomId;
    private final int sensorCount;

    public RoomNotEmptyException(String roomId, int sensorCount) {
        super("Room " + roomId + " still has " + sensorCount
              + " sensor(s) attached and cannot be deleted.");
        this.roomId = roomId;
        this.sensorCount = sensorCount;
    }

    public String getRoomId()      { return roomId; }
    public int    getSensorCount() { return sensorCount; }
}
