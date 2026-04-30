package cw.iot.smartcampus;

import cw.iot.smartcampus.endpoints.RootEndpoint;
import cw.iot.smartcampus.endpoints.SensorResource;
import cw.iot.smartcampus.endpoints.SensorRoom;
import cw.iot.smartcampus.observability.AuditFilter;
import cw.iot.smartcampus.problems.handlers.BadLinkMapper;
import cw.iot.smartcampus.problems.handlers.RoomConflictMapper;
import cw.iot.smartcampus.problems.handlers.SafetyNetMapper;
import cw.iot.smartcampus.problems.handlers.SensorBlockedMapper;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

/**
 * The JAX-RS Application subclass required by the brief.
 *
 * I prefer registering classes explicitly over relying on classpath
 * scanning (ResourceConfig.packages(...)) - it makes it crystal clear
 * which classes are part of the API and stops Jersey from picking up
 * something I forgot about. Slightly more typing, less surprise.
 */
@ApplicationPath("/api/v1")
public class RestApp extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> reg = new HashSet<>();

        // resource classes
        reg.add(RootEndpoint.class);
        reg.add(SensorRoom.class);
        reg.add(SensorResource.class);
        // ReadingsLog is mounted via sub-resource locator, no need to register

        // exception mappers (Part 5)
        reg.add(RoomConflictMapper.class);
        reg.add(BadLinkMapper.class);
        reg.add(SensorBlockedMapper.class);
        reg.add(SafetyNetMapper.class);

        // observability (Part 5.5)
        reg.add(AuditFilter.class);

        return reg;
    }
}
