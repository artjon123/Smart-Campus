package cw.iot.smartcampus.problems;

// Raised when the body of a request points at another resource that
// does not exist - in our case, posting a Sensor whose roomId is not
// in the store. The brief explicitly asks for this exception by name
// in part 5.2.
public class LinkedResourceNotFoundException extends RuntimeException {

    private final String linkField;     // e.g. "roomId"
    private final String linkValue;     // e.g. "LIB-999"

    public LinkedResourceNotFoundException(String linkField, String linkValue) {
        super("Referenced resource for '" + linkField + "' = '" + linkValue
              + "' could not be found.");
        this.linkField = linkField;
        this.linkValue = linkValue;
    }

    public String getLinkField() { return linkField; }
    public String getLinkValue() { return linkValue; }
}
