package cw.iot.smartcampus.problems;

// Thrown when somebody tries to push a reading to a sensor whose status
// is not ACTIVE - typically MAINTENANCE or OFFLINE. The brief calls for
// 403 Forbidden in this case (part 5.3) which sounds odd at first because
// the client *is* allowed to do POST in general; what is forbidden is
// posting *right now*, on *this* resource, in *this* state.
public class SensorUnavailableException extends RuntimeException {

    private final String sensorId;
    private final String currentStatus;

    public SensorUnavailableException(String sensorId, String currentStatus) {
        super("Sensor " + sensorId + " is currently '" + currentStatus
              + "' and cannot accept new readings.");
        this.sensorId      = sensorId;
        this.currentStatus = currentStatus;
    }

    public String getSensorId()      { return sensorId; }
    public String getCurrentStatus() { return currentStatus; }
}
