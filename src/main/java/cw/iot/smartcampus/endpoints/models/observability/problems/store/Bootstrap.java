package cw.iot.smartcampus;

import cw.iot.smartcampus.store.AppState;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.net.URI;

/**
 * The main(...) entry point. Starts an embedded Grizzly server and
 * mounts our JAX-RS application on it.
 *
 * I went with Grizzly for two practical reasons:
 *   1. it has a one-line factory call - no servlet container or web.xml
 *      needed, which keeps the project tiny.
 *   2. the brief explicitly allows "a lightweight servlet container or
 *      embedded server" so this is in scope.
 *
 * The base URI ends with /api/v1/ but RestApp also has @ApplicationPath
 * set to /api/v1. Jersey is happy with the duplication - the request
 * paths still resolve correctly - and having both means if I ever
 * accidentally drop the @ApplicationPath the URLs still work.
 */
public final class Bootstrap {

    private static final int    PORT     = 8080;
    // GrizzlyHttpServerFactory does not honour @ApplicationPath at runtime,
    // so the path prefix has to be baked into the base URI here. The
    // RestApp class still has @ApplicationPath("/api/v1") for the JAX-RS
    // standard's sake (and so it works under a servlet container too).
    private static final String BASE_URI = "http://localhost:" + PORT + "/api/v1/";

    private Bootstrap() { /* not instantiable */ }

    public static void main(String[] args) throws Exception {

        // load some seed rooms / sensors so the API isn't empty when
        // the marker first opens it
        AppState.bootstrap();

        // ResourceConfig.forApplicationClass(...) wires Jersey up to
        // our explicit Application subclass instead of doing classpath
        // scanning - matches the registration we did in RestApp.
        ResourceConfig rc = ResourceConfig.forApplicationClass(RestApp.class);

        HttpServer server = GrizzlyHttpServerFactory
                .createHttpServer(URI.create(BASE_URI), rc, false);

        // Hook a clean shutdown so Ctrl+C doesn't leave a half-open port
        // stderr avoids the jansi AnsiOutputStream crash on Windows
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.err.println("\nShutting down Smart Campus API...");
            server.shutdownNow();
        }));

        server.start();

        System.out.println("================================================");
        System.out.println(" Smart Campus API is up");
        System.out.println(" Discovery:  " + BASE_URI);
        System.out.println(" Rooms:      " + BASE_URI + "rooms");
        System.out.println(" Sensors:    " + BASE_URI + "sensors");
        System.out.println(" Press Ctrl+C to stop.");
        System.out.println("================================================");

        // park the main thread - if we returned, the JVM would exit
        // because Grizzly's threads are daemons
        Thread.currentThread().join();
    }
}
