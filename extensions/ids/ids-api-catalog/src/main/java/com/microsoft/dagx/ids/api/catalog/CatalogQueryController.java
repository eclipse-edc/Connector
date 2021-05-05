package com.microsoft.dagx.ids.api.catalog;

import com.microsoft.dagx.ids.spi.daps.DapsService;
import com.microsoft.dagx.spi.types.domain.metadata.DataEntry;
import de.fraunhofer.iais.eis.QueryMessage;
import de.fraunhofer.iais.eis.RejectionMessageBuilder;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import static de.fraunhofer.iais.eis.RejectionReason.MALFORMED_MESSAGE;
import static de.fraunhofer.iais.eis.RejectionReason.NOT_AUTHENTICATED;
import static java.util.stream.Collectors.toList;

/**
 * Handles incoming client data catalog queries.
 */
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/ids")
public class CatalogQueryController {
    private QueryEngine queryEngine;
    private DapsService dapsService;

    public CatalogQueryController(QueryEngine queryEngine, DapsService dapsService) {
        this.queryEngine = queryEngine;
        this.dapsService = dapsService;
    }

    @POST
    @Path("query")
    public Response query(QueryMessage message) {
        var connectorId = message.getIssuerConnector().toString();

        var verificationResult = dapsService.verifyAndConvertToken(message.getSecurityToken().getTokenValue());
        if (!verificationResult.valid()) {
            return Response.status(Response.Status.FORBIDDEN).entity(new RejectionMessageBuilder()._rejectionReason_(NOT_AUTHENTICATED).build()).build();
        }

        var query = (String) message.getProperties().get("query");
        if (query == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity(new RejectionMessageBuilder()._rejectionReason_(MALFORMED_MESSAGE).build()).build();
        }

        var language = (String) message.getProperties().get("queryLanguage");
        if (language == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity(new RejectionMessageBuilder()._rejectionReason_(MALFORMED_MESSAGE).build()).build();
        }

        var correlationId = message.getId().toString();

        var clientToken = verificationResult.token();

        var results = queryEngine.execute(correlationId, clientToken, connectorId, language, query);
        return Response.ok(results.stream().map(DataEntry::getId).collect(toList())).build();
    }
}
