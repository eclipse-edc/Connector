package com.microsoft.dagx.ids.api.transfer;

import com.microsoft.dagx.ids.spi.daps.DapsService;
import com.microsoft.dagx.spi.iam.VerificationResult;
import com.microsoft.dagx.spi.metadata.MetadataStore;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.transfer.TransferManager;
import com.microsoft.dagx.spi.transfer.TransferManagerRegistry;
import com.microsoft.dagx.spi.transfer.TransferResponse;
import com.microsoft.dagx.spi.types.domain.metadata.DataEntry;
import com.microsoft.dagx.spi.types.domain.transfer.DataRequest;
import de.fraunhofer.iais.eis.ArtifactRequestMessage;
import de.fraunhofer.iais.eis.ArtifactResponseMessageBuilder;
import de.fraunhofer.iais.eis.RejectionMessageBuilder;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import static de.fraunhofer.iais.eis.RejectionReason.INTERNAL_RECIPIENT_ERROR;
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
    private DapsService dapsService;
    private MetadataStore metadataStore;
    private TransferManagerRegistry transferManagerRegistry;
    private Monitor monitor;

    public ArtifactRequestController(DapsService dapsService,
                                     MetadataStore metadataStore,
                                     TransferManagerRegistry transferManagerRegistry,
                                     Monitor monitor) {
        this.dapsService = dapsService;
        this.metadataStore = metadataStore;
        this.transferManagerRegistry = transferManagerRegistry;
        this.monitor = monitor;
    }

    @POST
    @Path("request")
    public Response request(ArtifactRequestMessage message) {

        VerificationResult verificationResult = dapsService.verifyAndConvertToken(message.getSecurityToken().getTokenValue());
        if (!verificationResult.valid()) {
            return Response.status(Response.Status.FORBIDDEN).entity(new RejectionMessageBuilder()._rejectionReason_(NOT_AUTHENTICATED).build()).build();
        }

        // TODO enforce policy

        var dataUrn = message.getRequestedArtifact().toString();
        monitor.debug(() -> "Received artifact request for: " + dataUrn);

        DataEntry<?> entry = metadataStore.findForId(dataUrn);

        if (entry == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity(new RejectionMessageBuilder()._rejectionReason_(NOT_FOUND).build()).build();
        }

        DataRequest dataRequest = DataRequest.Builder.newInstance().id(randomUUID().toString()).dataEntry(entry).build();

        TransferManager transferManager = transferManagerRegistry.getManager(dataRequest);
        if (transferManager == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity(new RejectionMessageBuilder()._rejectionReason_(INTERNAL_RECIPIENT_ERROR).build()).build();
        }

        TransferResponse response = transferManager.initiateTransfer(dataRequest);

        if (response.getStatus() == TransferResponse.Status.OK) {
            monitor.info("Data transfer request initiated");
            ArtifactResponseMessageBuilder messageBuilder = new ArtifactResponseMessageBuilder();
            return Response.ok().entity(messageBuilder.build()).build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST).entity(new RejectionMessageBuilder()._rejectionReason_(TEMPORARILY_NOT_AVAILABLE).build()).build();
        }
    }


}
