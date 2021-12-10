/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.api.transfer;

import de.fraunhofer.iais.eis.ArtifactRequestMessage;
import de.fraunhofer.iais.eis.ArtifactResponseMessageBuilder;
import de.fraunhofer.iais.eis.RejectionMessageBuilder;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.dataspaceconnector.ids.spi.daps.DapsService;
import org.eclipse.dataspaceconnector.ids.spi.policy.IdsPolicyService;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.policy.PolicyRegistry;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessManager;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;

import java.util.Map;

import static de.fraunhofer.iais.eis.RejectionReason.BAD_PARAMETERS;
import static de.fraunhofer.iais.eis.RejectionReason.NOT_AUTHENTICATED;
import static de.fraunhofer.iais.eis.RejectionReason.NOT_AUTHORIZED;
import static de.fraunhofer.iais.eis.RejectionReason.NOT_FOUND;
import static de.fraunhofer.iais.eis.RejectionReason.TEMPORARILY_NOT_AVAILABLE;
import static java.util.UUID.randomUUID;
import static org.eclipse.dataspaceconnector.ids.spi.Protocols.IDS_REST;

/**
 * Receives incoming data transfer requests and processes them.
 */
@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@Path("/ids")
public class ArtifactRequestController {
    private static final String TOKEN_KEY = "dataspaceconnector-destination-token";
    private static final String DESTINATION_KEY = "dataspaceconnector-data-destination";
    private static final String PROPERTIES_KEY = "dataspaceconnector-properties";

    private final DapsService dapsService;
    private final AssetIndex assetIndex;
    private final TransferProcessManager processManager;
    private final IdsPolicyService policyService;
    private final PolicyRegistry policyRegistry;
    private final Vault vault;
    private final Monitor monitor;

    public ArtifactRequestController(DapsService dapsService,
                                     AssetIndex assetIndex,
                                     TransferProcessManager processManager,
                                     IdsPolicyService policyService,
                                     PolicyRegistry policyRegistry,
                                     Vault vault,
                                     Monitor monitor) {
        this.dapsService = dapsService;
        this.assetIndex = assetIndex;
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

        var asset = assetIndex.findById(dataUrn);

        if (asset == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity(new RejectionMessageBuilder()._rejectionReason_(NOT_FOUND).build()).build();
        }

        var policy = policyRegistry.resolvePolicy(asset.getPolicyId());
        if (policy == null) {
            monitor.severe("Policy not found for artifact: " + dataUrn);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new RejectionMessageBuilder()._rejectionReason_(TEMPORARILY_NOT_AVAILABLE).build()).build();
        }

        var consumerConnectorId = message.getIssuerConnector().toString();
        var correlationId = message.getId().toString();
        var policyResult = policyService.evaluateRequest(consumerConnectorId, correlationId, verificationResult.token(), policy);

        if (!policyResult.valid()) {
            monitor.info("Policy evaluation failed");
            return Response.status(Response.Status.FORBIDDEN).entity(new RejectionMessageBuilder()._rejectionReason_(NOT_AUTHORIZED).build()).build();
        }

        Map<String, Object> messageProperties = message.getProperties();
        var destinationMap = (Map<String, Object>) messageProperties.get(DESTINATION_KEY);
        var type = (String) destinationMap.get("type");

        Map<String, String> destinationProperties = (Map<String, String>) destinationMap.get("properties");
        var secretName = (String) destinationMap.get("keyName");

        var dataDestination = DataAddress.Builder.newInstance().type(type).properties(destinationProperties).keyName(secretName).build();

        Map<String, String> requestProperties = (Map<String, String>) messageProperties.get(PROPERTIES_KEY);

        var dataRequest = DataRequest.Builder.newInstance()
                .id(randomUUID().toString())
                .assetId(asset.getId())
                .dataDestination(dataDestination)
                .protocol(IDS_REST)
                .properties(requestProperties)
                .build();

        var destinationToken = (String) messageProperties.get(TOKEN_KEY);

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
