package cw.iot.smartcampus.endpoints;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.LinkedHashMap;
import java.util.Map;

// Discovery endpoint required by Part 1.2. Hitting GET /api/v1 returns
// a small JSON document that tells the caller the API version, who to
// talk to, and the URLs of the main collections. This is the simplest
// form of HATEOAS - the client doesn't have to hard-code paths, it
// can follow the links from this single entry point.
@Path("/")
public class RootEndpoint {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> describe() {
        // LinkedHashMap is used so the JSON keys come out in a
        // predictable order for screenshots / report
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("api",         "Smart Campus API");
        root.put("version",     "v1");
        root.put("module",      "5COSC022W Client-Server Architectures");
        root.put("contact",     "campus-it@example.ac.uk");

        Map<String, String> links = new LinkedHashMap<>();
        links.put("self",       "/api/v1");
        links.put("rooms",      "/api/v1/rooms");
        links.put("sensors",    "/api/v1/sensors");
        // readings live under sensors so I link the parent collection
        // and the conventional sub-path so anyone reading the discovery
        // doc knows the shape of the URL hierarchy
        links.put("readingsFor","/api/v1/sensors/{sensorId}/readings");
        root.put("resources", links);

        return root;
    }
}
