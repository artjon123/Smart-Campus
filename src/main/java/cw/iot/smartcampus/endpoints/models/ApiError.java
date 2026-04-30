package cw.iot.smartcampus.models;

import java.time.Instant;

// Whatever exception is thrown inside a resource, the matching
// ExceptionMapper turns it into one of these and that is what the client
// sees. Having one shared shape means a client only has to learn one
// error format.
//
// I deliberately do not include any stack trace or class name fields -
// leaking those is the security mistake the brief warns about in part 5.4.
public class ApiError {

    private int status;          // numeric HTTP code, e.g. 409
    private String reason;       // short label, e.g. "Conflict"
    private String message;
    private String path;         // request URI for context in the logs
    private String when;         // ISO-8601 timestamp

    public ApiError() { }

    public ApiError(int status, String reason, String message, String path) {
        this.status   = status;
        this.reason   = reason;
        this.message  = message;
        this.path     = path;
        this.when     = Instant.now().toString();
    }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getWhen() { return when; }
    public void setWhen(String when) { this.when = when; }
}
