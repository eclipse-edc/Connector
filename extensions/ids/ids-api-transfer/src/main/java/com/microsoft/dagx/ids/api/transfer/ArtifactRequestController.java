package com.microsoft.dagx.ids.api.transfer;

import com.microsoft.dagx.ids.spi.daps.DapsService;
import com.microsoft.dagx.spi.iam.ClaimToken;
import com.microsoft.dagx.spi.iam.VerificationResult;
import com.microsoft.dagx.spi.metadata.MetadataStore;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.security.Vault;
import com.microsoft.dagx.spi.transfer.TransferProcessManager;
import com.microsoft.dagx.spi.types.TypeManager;
import com.microsoft.dagx.spi.types.domain.transfer.DataAddress;
import com.microsoft.dagx.spi.types.domain.transfer.DataRequest;
import com.microsoft.dagx.spi.types.domain.transfer.DestinationSecretToken;
import de.fraunhofer.iais.eis.ArtifactRequestMessage;
import de.fraunhofer.iais.eis.ArtifactResponseMessageBuilder;
import de.fraunhofer.iais.eis.RejectionMessageBuilder;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;

import static com.microsoft.dagx.ids.spi.Protocols.IDS_REST;
import static com.microsoft.dagx.spi.util.Cast.cast;
import static de.fraunhofer.iais.eis.RejectionReason.BAD_PARAMETERS;
import static de.fraunhofer.iais.eis.RejectionReason.NOT_AUTHENTICATED;
import static de.fraunhofer.iais.eis.RejectionReason.NOT_FOUND;
import static de.fraunhofer.iais.eis.RejectionReason.TEMPORARILY_NOT_AVAILABLE;
import static java.util.UUID.randomUUID;

/**
 * Receives incoming data transfer requests and processes them.
 */
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/ids")
public class ArtifactRequestController {
    private static final String TOKEN_KEY = "dagx-destination-token";
    private static final String DESTINATION_KEY = "dagx-data-destination";

    private DapsService dapsService;
    private MetadataStore metadataStore;
    private TransferProcessManager processManager;
    private Vault vault;
    private TypeManager typeManager;
    private Monitor monitor;

    public ArtifactRequestController(DapsService dapsService, MetadataStore metadataStore, TransferProcessManager processManager, Vault vault, TypeManager typeManager, Monitor monitor) {
        this.dapsService = dapsService;
        this.metadataStore = metadataStore;
        this.processManager = processManager;
        this.vault = vault;
        this.typeManager = typeManager;
        this.monitor = monitor;
    }

    @POST
    @Path("request")
    public Response request(ArtifactRequestMessage message) {
        var verificationResult = new VerificationResult((ClaimToken) null);
//        var verificationResult = dapsService.verifyAndConvertToken(message.getSecurityToken().getTokenValue());
        if (!verificationResult.valid()) {
            return Response.status(Response.Status.FORBIDDEN).entity(new RejectionMessageBuilder()._rejectionReason_(NOT_AUTHENTICATED).build()).build();
        }

        // TODO enforce policy

        var dataUrn = message.getRequestedArtifact().toString();
        monitor.debug(() -> "Received artifact request for: " + dataUrn);

        var entry = metadataStore.findForId(dataUrn);

        if (entry == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity(new RejectionMessageBuilder()._rejectionReason_(NOT_FOUND).build()).build();
        }

        // TODO this needs to be deserialized from the artifact request message
        @SuppressWarnings("unchecked") var destinationMap = (Map<String, String>) message.getProperties().get(DESTINATION_KEY);
        var dataDestination = DataAddress.Builder.newInstance().type(destinationMap.get("type")).properties(cast(destinationMap.get("properties"))).build();

        var dataRequest = DataRequest.Builder.newInstance().id(randomUUID().toString()).dataEntry(entry).dataDestination(dataDestination).protocol(IDS_REST).build();

        var destinationToken = (String) message.getProperties().get(TOKEN_KEY);

        if (destinationToken != null) {
            // On the provider, use request id to store the token instead of the process id since the latter is not available until the process has been persisted.
            // The token cannot be saved after the process is persisted as that could introduce a race condition where the token is not yet stored when the process is initiated
            vault.storeSecret(DestinationSecretToken.KEY + "-" + dataRequest.getId(), typeManager.writeValueAsString(destinationToken));
        }

        var response = processManager.initiateProviderRequest(dataRequest);

        switch (response.getStatus()) {
            case OK:
                monitor.info("Data transfer request initiated");
                ArtifactResponseMessageBuilder messageBuilder = new ArtifactResponseMessageBuilder();
                return Response.ok().entity(messageBuilder.build()).build();
            case FATAL_ERROR:
                return Response.status(Response.Status.BAD_REQUEST).entity(new RejectionMessageBuilder()._rejectionReason_(BAD_PARAMETERS).build()).build();
            default:
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new RejectionMessageBuilder()._rejectionReason_(TEMPORARILY_NOT_AVAILABLE).build()).build();
        }
    }

}
