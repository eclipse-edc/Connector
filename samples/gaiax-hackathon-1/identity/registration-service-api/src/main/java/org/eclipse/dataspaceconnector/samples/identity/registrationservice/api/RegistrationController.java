package org.eclipse.dataspaceconnector.samples.identity.registrationservice.api;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.dataspaceconnector.common.string.StringUtils;
import org.eclipse.dataspaceconnector.iam.ion.dto.did.DidDocument;
import org.eclipse.dataspaceconnector.iam.ion.spi.DidStore;
import org.eclipse.dataspaceconnector.spi.iam.RegistrationService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;

import java.util.List;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/identity")
public class RegistrationController implements RegistrationService {
    private final Monitor monitor;
    private final DidStore didDocumentStore;

    public RegistrationController(Monitor monitor, DidStore didDocumentStore) {
        this.monitor = monitor;
        this.didDocumentStore = didDocumentStore;
    }


    @GET
    @Path("/dids")
    public Response getDids(@QueryParam("since") String offset) {
        monitor.info("Fetching all DIDs");
        List<DidDocument> allDids;
        if (StringUtils.isNullOrBlank(offset)) {
            allDids = getAllDids(100);
        } else {
            allDids = getDidsWithOffset(offset);
        }

        return Response.ok(allDids).build();
    }

    private List<DidDocument> getDidsWithOffset(String offset) {
        return didDocumentStore.getAfter(offset);
    }

    private List<DidDocument> getAllDids(int maxNumber) {
        return didDocumentStore.getAll(maxNumber);
    }
}
