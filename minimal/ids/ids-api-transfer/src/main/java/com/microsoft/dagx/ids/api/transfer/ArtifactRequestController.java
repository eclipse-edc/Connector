/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.ids.api.transfer;

import com.microsoft.dagx.ids.spi.daps.DapsService;
import com.microsoft.dagx.ids.spi.policy.IdsPolicyService;
import com.microsoft.dagx.spi.metadata.MetadataStore;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.policy.PolicyRegistry;
import com.microsoft.dagx.spi.security.Vault;
import com.microsoft.dagx.spi.transfer.TransferProcessManager;
import com.microsoft.dagx.spi.types.domain.transfer.DataAddress;
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

import java.util.Map;

import static com.microsoft.dagx.common.Cast.cast;
import static com.microsoft.dagx.ids.spi.Protocols.IDS_REST;
import static de.fraunhofer.iais.eis.RejectionReason.*;
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

    private final DapsService dapsService;
    private final MetadataStore metadataStore;
    private final TransferProcessManager processManager;
    private final IdsPolicyService policyService;
    private final PolicyRegistry policyRegistry;
    private final Vault vault;
    private final Monitor monitor;

    public ArtifactRequestController(DapsService dapsService,
                                     MetadataStore metadataStore,
                                     TransferProcessManager processManager,
                                     IdsPolicyService policyService,
                                     PolicyRegistry policyRegistry,
                                     Vault vault,
                                     Monitor monitor) {
        this.dapsService = dapsService;
        this.metadataStore = metadataStore;
        this.processManager = processManager;
        this.policyService = policyService;
        this.policyRegistry = policyRegistry;
        this.vault = vault;
        this.monitor = monitor;
    }

    @POST
    @Path("request")
    public Response request(ArtifactRequestMessage message) {
        var verificationResult = dapsService.verifyAndConvertToken(message.getSecurityToken().getTokenValue());
        if (!verificationResult.valid()) {
            monitor.info(() -> "verification failed for request " + message.getId());
            return Response.status(Response.Status.FORBIDDEN).entity(new RejectionMessageBuilder()._rejectionReason_(NOT_AUTHENTICATED).build()).build();
        }

        var dataUrn = message.getRequestedArtifact().toString();
        monitor.debug(() -> "Received artifact request for: " + dataUrn);

        var entry = metadataStore.findForId(dataUrn);

        if (entry == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity(new RejectionMessageBuilder()._rejectionReason_(NOT_FOUND).build()).build();
        }

        var policy = policyRegistry.resolvePolicy(entry.getPolicyId());
        if (policy == null) {
            monitor.severe("Policy not found for artifact: " + dataUrn);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new RejectionMessageBuilder()._rejectionReason_(TEMPORARILY_NOT_AVAILABLE).build()).build();
        }

        var clientConnectorId = message.getIssuerConnector().toString();
        var correlationId = message.getId().toString();
        var policyResult = policyService.evaluateRequest(clientConnectorId, correlationId, verificationResult.token(), policy);

        if (!policyResult.valid()) {
            monitor.info("Policy evaluation failed");
            return Response.status(Response.Status.FORBIDDEN).entity(new RejectionMessageBuilder()._rejectionReason_(NOT_AUTHORIZED).build()).build();
        }


        // TODO this needs to be deserialized from the artifact request message
        @SuppressWarnings("unchecked") var destinationMap = (Map<String, Object>) message.getProperties().get(DESTINATION_KEY);
        var type = (String) destinationMap.get("type");

        @SuppressWarnings("unchecked") Map<String, String> properties = (Map<String, String>) destinationMap.get("properties");
        var secretName = (String) destinationMap.get("keyName");

        final Map<String, String> cast = cast(properties);
        var dataDestination = DataAddress.Builder.newInstance().type(type).properties(cast).keyName(secretName).build();

        var dataRequest = DataRequest.Builder.newInstance().id(randomUUID().toString()).dataEntry(entry).dataDestination(dataDestination).protocol(IDS_REST).build();

        var destinationToken = (String) message.getProperties().get(TOKEN_KEY);

        if (destinationToken != null) {
            vault.storeSecret(secretName, destinationToken);
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
