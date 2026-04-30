# Smart Campus Sensor & Room Management API

Coursework submission for **5COSC022W Client-Server Architectures** (2025/26).

This is a JAX-RS REST API that lets a campus facilities team (or another
automated system) manage the rooms on campus, the sensors deployed in
those rooms, and the historical readings each sensor has produced.
Everything is held in memory using `ConcurrentHashMap` / `ArrayList`,
in line with the brief which forbids using a real database.

---

## 1. API design overview

Resources and the URLs that point at them:

| Resource             | URL                                            | Methods                  |
|----------------------|------------------------------------------------|--------------------------|
| Discovery doc        | `/api/v1`                                      | `GET`                    |
| Rooms collection     | `/api/v1/rooms`                                | `GET`, `POST`            |
| Single room          | `/api/v1/rooms/{roomId}`                       | `GET`, `DELETE`          |
| Sensors collection   | `/api/v1/sensors` (also `?type=Temperature`)   | `GET`, `POST`            |
| Single sensor        | `/api/v1/sensors/{sensorId}`                   | `GET`                    |
| Reading history      | `/api/v1/sensors/{sensorId}/readings`          | `GET`, `POST`            |

Design choices worth pointing out:

- **Three-package layout** — `endpoints` (JAX-RS resources),
  `models` (POJOs), `store` (singleton in-memory store), `problems`
  (custom exceptions + mappers), `observability` (the request/response
  filter). Putting concerns in their own folder keeps the resource
  classes short.
- **Sub-resource locator pattern** for readings. `SensorResource` does
  not handle `GET /sensors/{id}/readings` itself — it delegates to a
  `ReadingsLog` instance via a locator method, which gives that
  sub-resource a direct handle on the parent sensor.
- **One uniform error shape** (`ApiError` JSON body) returned by every
  custom mapper and the global safety net. A client only ever has to
  parse one error format.
- **Default singleton resource lifecycle** is *not* used here. JAX-RS
  resource classes are per-request by default, which is what we keep.
  Shared state lives in `AppState` (a process-wide singleton) backed by
  `ConcurrentHashMap` so concurrent requests do not corrupt the maps.

### Tech stack
- Java 11 (compiles & runs on later versions, tested on JDK 21)
- JAX-RS 2.1 (`javax.ws.rs.*`)
- Jersey 2.41 as the JAX-RS implementation
- Embedded Grizzly HTTP server (no servlet container needed)
- Jackson 2.15 for JSON
- Maven 3.x for build, with the shade plugin to produce a single
  runnable jar

---

## 2. How to build and run

### Prerequisites
- JDK 11 or newer (`java -version`)
- Apache Maven 3.6+ (`mvn -version`)

### Build

```bash
cd westminster-cw-api
mvn package
```

This produces `target/westminster-cw-api.jar` — a fat jar containing
every dependency.

### Launch the server

Either of these works:

```bash
# Option A: run the shaded jar
java -jar target/westminster-cw-api.jar

# Option B: use the exec plugin during development
mvn exec:java
```

You should see:

```
================================================
 Smart Campus API is up
 Discovery:  http://localhost:8080/api/v1/
 Rooms:      http://localhost:8080/api/v1/rooms
 Sensors:    http://localhost:8080/api/v1/sensors
 Press Ctrl+C to stop.
================================================
```

The API is now listening on **port 8080**. Three rooms and three
sensors are seeded automatically so the collections are not empty
on first call.

To stop the server press **Ctrl+C** — there is a shutdown hook that
stops Grizzly cleanly.

---

## 3. Sample curl commands

These are the same commands I used while testing each part of the
coursework. They cover the success path and at least one error path
per resource.

### 3.1 Discovery
```bash
curl http://localhost:8080/api/v1
```
Returns a JSON document with API metadata and the URLs of the main
collections.

### 3.2 List all rooms
```bash
curl http://localhost:8080/api/v1/rooms
```

### 3.3 Create a new room (returns 201 Created + Location header)
```bash
curl -i -X POST http://localhost:8080/api/v1/rooms \
     -H "Content-Type: application/json" \
     -d '{"id":"AUD-100","name":"Auditorium","capacity":500}'
```

### 3.4 Filter sensors by type (query parameter)
```bash
curl "http://localhost:8080/api/v1/sensors?type=Temperature"
```

### 3.5 Submit a new reading for a sensor (sub-resource POST)
```bash
curl -X POST http://localhost:8080/api/v1/sensors/TEMP-001/readings \
     -H "Content-Type: application/json" \
     -d '{"value":22.7}'
```

### 3.6 Try to delete a room that still has sensors → **409 Conflict**
```bash
curl -i -X DELETE http://localhost:8080/api/v1/rooms/LIB-301
```

### 3.7 Try to register a sensor with a roomId that does not exist → **422 Unprocessable Entity**
```bash
curl -i -X POST http://localhost:8080/api/v1/sensors \
     -H "Content-Type: application/json" \
     -d '{"id":"BAD-1","type":"Temperature","status":"ACTIVE","currentValue":0,"roomId":"DOES-NOT-EXIST"}'
```

### 3.8 Try to push a reading to a MAINTENANCE sensor → **403 Forbidden**
```bash
curl -i -X POST http://localhost:8080/api/v1/sensors/OCC-014/readings \
     -H "Content-Type: application/json" \
     -d '{"value":1}'
```

---

## 4. Conceptual Report (answers to the questions in the brief)

### Part 1.1 — Default lifecycle of a JAX-RS resource class

By default JAX-RS gives every resource class a **per-request** lifecycle.
For each incoming HTTP request the runtime instantiates a fresh copy of
the resource class, injects the context fields (e.g. `@Context UriInfo`),
calls the handler method, then discards the instance. The class is *not*
treated as a singleton unless you explicitly mark it with `@Singleton`
or register it as one.

The practical impact on shared state is significant. If I had stored my
rooms map as a normal field directly inside `SensorRoom`, every request
would have started with an empty map — the previous instance is gone by
the time the next request arrives. To work around that I keep all data
in a single `AppState` class that uses the singleton pattern (private
constructor, one static `INSTANCE`). That object lives for the whole JVM
lifetime, so writes from request *N* are still there when request *N+1*
runs.

Because Grizzly handles requests on a thread pool, two requests can
happen at the same time. To prevent data races I use:
- `ConcurrentHashMap` for the room and sensor maps so single-key
  put/get/remove operations are already atomic;
- `Collections.synchronizedList(...)` for the per-sensor readings buckets;
- a `synchronized` block whenever I have a *multi-step* write that has
  to be all-or-nothing (e.g. saving a sensor and attaching it to its
  parent room — both have to happen or neither, otherwise the room's
  `sensorIds` list goes out of sync with the sensors map).

### Part 1.2 — Why HATEOAS / hypermedia is a hallmark of advanced REST

HATEOAS (Hypermedia As The Engine Of Application State) is the bit of
REST that says responses should not just contain data, they should also
contain the *links* the client needs to use next. My discovery endpoint
is a small example of that — instead of writing a separate Word document
that says "the rooms collection is at /api/v1/rooms", I include that URL
*inside* the API response.

The benefits compared to static documentation:

1. **The server is the source of truth.** If I move rooms from
   `/api/v1/rooms` to `/api/v2/rooms` next year, every client that uses
   the discovery endpoint will follow automatically. A static PDF would
   be out of date the moment I redeployed.
2. **Less coupling.** Clients only need to know the entry point. They
   discover everything else by following links. That makes it much easier
   to evolve the API without breaking existing consumers.
3. **Fewer bugs in client code.** Developers do not have to hand-craft
   URLs by string-concatenation, so they cannot forget the `/api/v1`
   prefix or mistype a path.

### Part 2.1 — Returning IDs vs full Room objects

I return full Room objects from `GET /rooms`. The implications either
way:

- **IDs only**: smaller payload (less network bandwidth), but every
  client that wants to display a list of rooms with their names has to
  follow the list with N extra `GET /rooms/{id}` calls. That is the
  classic *N+1 request problem*. For a campus with hundreds of rooms
  that is a lot of round trips, and most of them happen before the
  page can render.
- **Full objects**: bigger response but only **one** network round trip.
  In our scale (the brief talks about thousands of rooms but typical
  list views show maybe a few dozen at a time) the JSON payload is still
  tiny — single-digit kilobytes — and the latency win from avoiding the
  N follow-up requests dominates.

For a Smart Campus dashboard that almost always wants to show
human-readable names alongside the IDs, the second option is cheaper
overall. If a use case ever needs only the IDs (e.g. a script that
just wants to delete every empty room) it can ask for that explicitly
via a query parameter — not the default.

### Part 2.2 — Is DELETE idempotent here?

**Yes.** Idempotency means the *server state* after applying the
operation N times is the same as after applying it once. My DELETE works
like this:

1. First call against `/rooms/LIB-301`: room exists, sensor list is
   empty, room is removed. Response: `204 No Content`.
2. Second call against `/rooms/LIB-301`: room is gone, store returns
   no match, I respond `404 Not Found`. The state is unchanged — the
   room is still gone.
3. All further calls behave exactly like the second one.

The HTTP status code differs between the first and subsequent calls
(204 → 404), but that is fine — idempotency is about server state, not
the response code. The room was deleted by call 1; calls 2 onwards do
nothing destructive. If the room had sensors attached the first call
would have raised `RoomNotEmptyException` (409 Conflict) and the state
would still be unchanged, which is also consistent with idempotency.

### Part 3.1 — Consequences of `@Consumes(MediaType.APPLICATION_JSON)`

`@Consumes(APPLICATION_JSON)` tells JAX-RS that the method only accepts
request bodies with `Content-Type: application/json`. If a client sends
something else like `text/plain` or `application/xml`, JAX-RS does the
matching at routing time, fails to find a method that consumes the
client's content type, and immediately returns **HTTP 415 Unsupported
Media Type** without ever invoking my handler. I tested this in
section 3 of the curl examples above and confirmed it works.

That behaviour is great because it stops malformed or hostile input
from reaching my code. If I had written `@Consumes(MediaType.WILDCARD)`
instead, Jackson would have happily tried to parse a binary file as
JSON and either thrown a parse exception or — worse — silently
deserialised garbage into a `Sensor` object.

### Part 3.2 — `@QueryParam` vs path-based filtering

`GET /sensors?type=CO2` is better than `GET /sensors/type/CO2` for a few
practical reasons:

1. **The resource is the same**. In both URLs the underlying resource
   is "the collection of sensors". A query parameter says "give me a
   subset of that collection", which matches the semantics. A path
   segment implies a *different* resource called `type/CO2`, which is
   not really a thing.
2. **Composability**. Multiple filters compose naturally with query
   parameters: `?type=CO2&status=ACTIVE`. With paths the URL would
   explode into permutations like `/type/CO2/status/ACTIVE` and the
   ordering would matter.
3. **Optional vs mandatory**. `@QueryParam` is naturally optional.
   When the client omits it I return the whole list. With a path
   segment I would need two routes (`/sensors` and `/sensors/type/{x}`)
   that do almost the same thing, which is duplicate code.
4. **Caching**. Caches and proxies normalise URLs on path; query
   parameters are explicitly meant to vary the response of the same
   resource. The path-based design would pollute caches with what is
   really a filtered view.

### Part 4.1 — Architectural benefits of the Sub-Resource Locator pattern

A sub-resource locator is a method on a resource class that, instead of
handling a request itself, *returns another resource object* which then
handles the rest of the URI. In my code `SensorResource#readingsFor()`
does exactly that — when a request hits `/sensors/{id}/readings/...`
the runtime walks down through the locator and dispatches the rest of
the path on the returned `ReadingsLog`.

The benefits over a single fat controller:

- **Smaller, focused classes**. `SensorResource` only knows about
  sensors. `ReadingsLog` only knows about readings. Each class fits on
  one screen.
- **Context is passed once, not looked up repeatedly**. The locator
  hands the parent `Sensor` to `ReadingsLog`'s constructor, so its
  GET / POST methods can use the parent directly without doing another
  store lookup.
- **Cleaner extension**. If we add `/sensors/{id}/alerts` later it
  becomes another sibling sub-resource with its own class. We do not
  bloat the original SensorResource with a third unrelated set of
  methods.
- **Better testability**. `ReadingsLog` can be unit-tested by
  constructing it with a mock `Sensor`; we do not have to drive it
  through HTTP.

### Part 5.2 — Why HTTP 422 is more accurate than 404 here

A 404 is appropriate when the *URI itself* points at nothing — e.g. a
client requests `GET /rooms/HOGWARTS` and we have no such room. The
URI is the locator, the locator does not resolve, hence "not found".

The Part 5.2 scenario is different: the client posts to
`POST /sensors`, which is a perfectly valid URI, and the server *can*
process the request in principle. The problem is one specific *field
inside the JSON body* (`roomId`) refers to a room that does not exist.
Returning 404 there is misleading because it suggests the client should
go and check the URL they hit, when in reality the URL was correct and
it is the body that needs to be fixed.

422 Unprocessable Entity (RFC 4918, then promoted into the HTTP semantics
RFC) is the precise code for this: "the request was syntactically
fine, the URL was found, but I cannot proceed because the content is
semantically invalid". That gives the client the information it needs:
"the bug is in your payload, not in your routing".

### Part 5.4 — Risks of leaking Java stack traces

Returning a raw stack trace to an external API consumer is a well-known
information disclosure vulnerability. Things an attacker can learn from
a leaked trace:

- **Class and package names** — reveals which framework you use
  (e.g. `com.fasterxml.jackson.*` says you use Jackson, with whatever
  CVEs are public for that version) and the structure of your code.
- **Library versions** — sometimes the message itself includes a
  version, and even when it does not, package names and method
  signatures often pin a release down. An attacker can then look up
  known exploits for that version.
- **File paths** on the server (`at com.example.foo.Bar(Bar.java:42)`)
  — gives away the deploy layout, possibly the OS, and helps target
  follow-up attacks.
- **SQL queries or query fragments** if a `SQLException` is wrapped
  and rethrown — sometimes that contains table or column names and
  even literal values, which is a step away from injection attacks.
- **Internal assumptions** — `NullPointerException` traces often hint
  at which inputs were missing, helping the attacker find unchecked
  code paths.

My `SafetyNetMapper` deliberately returns a generic message ("Something
went wrong on our side. The incident has been logged.") to the client
and writes the full exception to the server log only, so that a
developer can debug while an external caller learns nothing.

### Part 5.5 — Why use JAX-RS filters for cross-cutting concerns

A filter runs once per request (and once per response) regardless of
which resource method is being called. Putting logging there rather
than inside every resource gives me:

- **No duplication.** I do not have to remember to add
  `LOG.info("...")` to every endpoint method, nor update twenty places
  if the log format changes.
- **Consistency.** Every request is logged the same way with the same
  fields. If I only logged inside resources I'd inevitably miss some,
  especially error paths where the resource throws before reaching
  any log statement.
- **Right place in the pipeline.** Filters run *after* JAX-RS has
  picked the response status — even when an exception mapper produced
  the response — so I get the final status code in the log. A
  `Logger.info()` inside a resource cannot see this; it runs before
  the exception mapper.
- **Separation of concerns.** A resource method should be about
  business logic. Logging, auth, request IDs, metrics — those are
  infrastructure. Filters keep them out of the way.

The same argument applies to the Exception Mappers I implemented in
this part: instead of every resource catching its own exceptions and
formatting an error JSON, the mappers turn each exception type into a
consistent response in one place.

---

## 5. Project layout

```
westminster-cw-api/
├── pom.xml
├── README.md
└── src/main/java/cw/iot/smartcampus/
    ├── Bootstrap.java              # main(): starts Grizzly
    ├── RestApp.java                # @ApplicationPath + class registry
    ├── endpoints/
    │   ├── RootEndpoint.java       # GET /api/v1     (discovery)
    │   ├── SensorRoom.java         # /rooms collection + single
    │   ├── SensorResource.java     # /sensors collection + single + locator
    │   └── ReadingsLog.java        # sub-resource for /sensors/{id}/readings
    ├── models/
    │   ├── Room.java
    │   ├── Sensor.java
    │   ├── SensorReading.java
    │   └── ApiError.java           # uniform error JSON
    ├── observability/
    │   └── AuditFilter.java        # request + response logger
    ├── problems/
    │   ├── RoomNotEmptyException.java
    │   ├── LinkedResourceNotFoundException.java
    │   ├── SensorUnavailableException.java
    │   └── handlers/
    │       ├── RoomConflictMapper.java     # 409
    │       ├── BadLinkMapper.java          # 422
    │       ├── SensorBlockedMapper.java    # 403
    │       └── SafetyNetMapper.java        # 500 / catch-all
    └── store/
        └── AppState.java           # singleton in-memory store + seed data
```

---
