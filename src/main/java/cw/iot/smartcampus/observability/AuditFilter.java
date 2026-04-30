package cw.iot.smartcampus.observability;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * One filter, both directions - implements ContainerRequestFilter for
 * incoming and ContainerResponseFilter for outgoing.
 *
 * Pulled into its own class because logging is a textbook cross-cutting
 * concern: it shouldn't be duplicated inside every resource method, and
 * if I ever change the format I only have to touch one file.
 *
 * Logger is from java.util.logging as required by the brief, no SLF4J
 * or anything else.
 */
@Provider
public class AuditFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOG = Logger.getLogger("smart-campus-audit");

    /* incoming */
    @Override
    public void filter(ContainerRequestContext ctx) throws IOException {
        String method = ctx.getMethod();
        String uri    = ctx.getUriInfo().getRequestUri().toString();
        LOG.info("--> " + method + " " + uri);
    }

    /* outgoing */
    @Override
    public void filter(ContainerRequestContext  reqCtx,
                       ContainerResponseContext resCtx) throws IOException {
        int status = resCtx.getStatus();
        String method = reqCtx.getMethod();
        String path   = reqCtx.getUriInfo().getPath();
        LOG.info("<-- " + method + " /" + path + " " + status);
    }
}
