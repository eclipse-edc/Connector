/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.test.e2e.transfer;

import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.controlplane.test.system.utils.Participants;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.ManagementApiClientV5;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.AssetDto;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.DataPlaneRegistrationDto;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.PermissionDto;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.PolicyDefinitionDto;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.PolicyDto;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.AuthorizationProfile;
import org.eclipse.edc.junit.annotations.Runtime;
import org.eclipse.edc.signaling.auth.Oauth2Extension;
import org.eclipse.edc.signaling.client.DataPlaneSignalingTestClient;
import org.eclipse.edc.spi.EdcException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.COMPLETED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.SUSPENDED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.TERMINATED;
import static org.eclipse.edc.test.e2e.TransferEndToEndTestBase.CONSUMER_DP;


@SuppressWarnings("JUnitMalformedDeclaration")
public abstract class VirtualTransferEndToEndTestBase {

    public static final String PROVIDER_DP = "provider-data-plane";
    public static final String PROVIDER_CONTEXT = "provider";
    public static final String CONSUMER_CONTEXT = "consumer";
    public static final String PROVIDER_ID = "provider-id";
    public static final String CONSUMER_ID = "consumer-id";

    @BeforeAll
    static void beforeAll(ManagementApiClientV5 connectorClient,
                          Participants participants,
                          @Runtime(PROVIDER_DP) DataPlaneSignalingTestClient providerDataPlane,
                          @Runtime(CONSUMER_DP) DataPlaneSignalingTestClient consumerDataPlane,
                          Oauth2Extension oauth2) {
        connectorClient.createParticipant(participants.consumer().contextId(), participants.consumer().id(), participants.consumer().config());
        connectorClient.createParticipant(participants.provider().contextId(), participants.provider().id(), participants.provider().config());

        var consumerDataPlaneOauth2Profile = oauth2.registerClient(consumerDataPlane.dataPlaneId());
        var consumerControlPlaneOauth2Profile = oauth2.registerClient(participants.consumer().contextId());
        var providerDataPlaneOauth2Profile = oauth2.registerClient(providerDataPlane.dataPlaneId());
        var providerControlPlaneOauth2Profile = oauth2.registerClient(participants.provider().contextId());


        var consumerDp = new DataPlaneRegistrationDto(
                consumerDataPlane.dataPlaneId(),
                consumerDataPlane.getDataFlowsEndpoint(),
                Set.of("NonFinite-PULL"),
                Set.of(),
                toAuthorizationProfile(consumerControlPlaneOauth2Profile).properties()
        );
        connectorClient.dataplanes().registerDataPlane(participants.consumer().contextId(), consumerDp);

        var providerDp = new DataPlaneRegistrationDto(
                providerDataPlane.dataPlaneId(),
                providerDataPlane.getDataFlowsEndpoint(),
                Set.of("NonFinite-PULL"),
                Set.of(),
                toAuthorizationProfile(providerControlPlaneOauth2Profile).properties()
        );

        connectorClient.dataplanes().registerDataPlane(participants.provider().contextId(), providerDp);

        providerDataPlane.registerControlPlane(createObjectBuilder()
                .add("controlplaneId", participants.provider().contextId())
                .add("endpoint", participants.provider().getSignalingEndpointUrl())
                .add("authorization", createArrayBuilder().add(providerDataPlaneOauth2Profile))
                .build());

        consumerDataPlane.registerControlPlane(createObjectBuilder()
                .add("controlplaneId", participants.consumer().contextId())
                .add("endpoint", participants.consumer().getSignalingEndpointUrl())
                .add("authorization", createArrayBuilder().add(consumerDataPlaneOauth2Profile))
                .build());
    }

    private static AuthorizationProfile toAuthorizationProfile(JsonObject object) {
        var properties = new HashMap<String, Object>();
        object.forEach((key, value) -> {
            if (value instanceof JsonString s) {
                properties.put(key, s.getString());
            }
        });
        return new AuthorizationProfile(object.getString("type"), properties);
    }

    @Test
    void transfer(ManagementApiClientV5 connectorClient, Participants participants) {
        var providerAddress = participants.provider().getProtocolEndpoint();

        var assetId = setup(connectorClient, participants.provider());
        var transferProcessId = connectorClient.startTransfer(participants.consumer().contextId(), participants.provider().contextId(), providerAddress, participants.provider().id(), assetId, "NonFinite-PULL");

        var consumerTransfer = connectorClient.transfers().getTransferProcess(participants.consumer().contextId(), transferProcessId);
        var providerTransfer = connectorClient.transfers().getTransferProcess(participants.provider().contextId(), consumerTransfer.getCorrelationId());

        assertThat(consumerTransfer.getState()).isEqualTo(providerTransfer.getState());

    }

    @Test
    void suspendAndResumeByProvider(ManagementApiClientV5 connectorClient, Participants participants) {
        var providerAddress = participants.provider().getProtocolEndpoint();

        var assetId = setup(connectorClient, participants.provider());
        var transferProcessId = connectorClient.startTransfer(participants.consumer().contextId(), participants.provider().contextId(), providerAddress, participants.provider().id(), assetId, "NonFinite-PULL");

        var consumerTransfer = connectorClient.transfers().getTransferProcess(participants.consumer().contextId(), transferProcessId);
        var providerTransfer = connectorClient.transfers().getTransferProcess(participants.provider().contextId(), consumerTransfer.getCorrelationId());

        assertThat(consumerTransfer.getState()).isEqualTo(providerTransfer.getState());

        connectorClient.transfers().suspendTransfer(participants.provider().contextId(), consumerTransfer.getCorrelationId(), "Suspending for test");

        connectorClient.waitTransferInState(participants.consumer().contextId(), transferProcessId, SUSPENDED);
        connectorClient.waitTransferInState(participants.provider().contextId(), consumerTransfer.getCorrelationId(), SUSPENDED);


        connectorClient.transfers().resumeTransfer(participants.provider().contextId(), consumerTransfer.getCorrelationId());

        connectorClient.waitTransferInState(participants.consumer().contextId(), transferProcessId, STARTED);
        connectorClient.waitTransferInState(participants.provider().contextId(), consumerTransfer.getCorrelationId(), STARTED);
    }

    @Test
    void suspendAndResumeByConsumer(ManagementApiClientV5 connectorClient, Participants participants) {
        var providerAddress = participants.provider().getProtocolEndpoint();

        var assetId = setup(connectorClient, participants.provider());
        var transferProcessId = connectorClient.startTransfer(participants.consumer().contextId(), participants.provider().contextId(), providerAddress, participants.provider().id(), assetId, "NonFinite-PULL");

        var consumerTransfer = connectorClient.transfers().getTransferProcess(participants.consumer().contextId(), transferProcessId);
        var providerTransfer = connectorClient.transfers().getTransferProcess(participants.provider().contextId(), consumerTransfer.getCorrelationId());

        assertThat(consumerTransfer.getState()).isEqualTo(providerTransfer.getState());

        connectorClient.transfers().suspendTransfer(participants.consumer().contextId(), transferProcessId, "Suspending for test");

        connectorClient.waitTransferInState(participants.consumer().contextId(), transferProcessId, SUSPENDED);
        connectorClient.waitTransferInState(participants.provider().contextId(), consumerTransfer.getCorrelationId(), SUSPENDED);

        connectorClient.transfers().resumeTransfer(participants.consumer().contextId(), transferProcessId);

        connectorClient.waitTransferInState(participants.consumer().contextId(), transferProcessId, STARTED);
        connectorClient.waitTransferInState(participants.provider().contextId(), consumerTransfer.getCorrelationId(), STARTED);
    }

    @Test
    void terminateByConsumer(ManagementApiClientV5 connectorClient, Participants participants) {
        var providerAddress = participants.provider().getProtocolEndpoint();

        var assetId = setup(connectorClient, participants.provider());
        var transferProcessId = connectorClient.startTransfer(participants.consumer().contextId(), participants.provider().contextId(), providerAddress, participants.provider().id(), assetId, "NonFinite-PULL");

        var consumerTransfer = connectorClient.transfers().getTransferProcess(participants.consumer().contextId(), transferProcessId);
        var providerTransfer = connectorClient.transfers().getTransferProcess(participants.provider().contextId(), consumerTransfer.getCorrelationId());

        assertThat(consumerTransfer.getState()).isEqualTo(providerTransfer.getState());

        connectorClient.transfers().terminateTransfer(participants.consumer().contextId(), transferProcessId, "Terminate for test");

        connectorClient.waitTransferInState(participants.consumer().contextId(), transferProcessId, TERMINATED);
        connectorClient.waitTransferInState(participants.provider().contextId(), consumerTransfer.getCorrelationId(), TERMINATED);

    }

    @Test
    void terminateByProvider(ManagementApiClientV5 connectorClient, Participants participants) {
        var providerAddress = participants.provider().getProtocolEndpoint();

        var assetId = setup(connectorClient, participants.provider());
        var transferProcessId = connectorClient.startTransfer(participants.consumer().contextId(), participants.provider().contextId(), providerAddress, participants.provider().id(), assetId, "NonFinite-PULL");

        var consumerTransfer = connectorClient.transfers().getTransferProcess(participants.consumer().contextId(), transferProcessId);
        var providerTransfer = connectorClient.transfers().getTransferProcess(participants.provider().contextId(), consumerTransfer.getCorrelationId());

        assertThat(consumerTransfer.getState()).isEqualTo(providerTransfer.getState());

        connectorClient.transfers().terminateTransfer(participants.consumer().contextId(), transferProcessId, "Suspending for test");

        connectorClient.waitTransferInState(participants.consumer().contextId(), transferProcessId, TERMINATED);
        connectorClient.waitTransferInState(participants.provider().contextId(), consumerTransfer.getCorrelationId(), TERMINATED);

    }

    @Test
    void completeByProvider(TransferProcessService service, ManagementApiClientV5 connectorClient, Participants participants) {
        var providerAddress = participants.provider().getProtocolEndpoint();

        var assetId = setup(connectorClient, participants.provider());
        var transferProcessId = connectorClient.startTransfer(participants.consumer().contextId(), participants.provider().contextId(), providerAddress, participants.provider().id(), assetId, "NonFinite-PULL");

        var consumerTransfer = connectorClient.transfers().getTransferProcess(participants.consumer().contextId(), transferProcessId);
        var providerTransfer = connectorClient.transfers().getTransferProcess(participants.provider().contextId(), consumerTransfer.getCorrelationId());

        assertThat(consumerTransfer.getState()).isEqualTo(providerTransfer.getState());


        service.complete(consumerTransfer.getCorrelationId())
                .orElseThrow(f -> new EdcException(f.getFailureDetail()));

        connectorClient.waitTransferInState(participants.consumer().contextId(), transferProcessId, COMPLETED);
        connectorClient.waitTransferInState(participants.provider().contextId(), consumerTransfer.getCorrelationId(), COMPLETED);

    }

    @Test
    void completeByConsumer(TransferProcessService service, ManagementApiClientV5 connectorClient, Participants participants) {
        var providerAddress = participants.provider().getProtocolEndpoint();

        var assetId = setup(connectorClient, participants.provider());
        var transferProcessId = connectorClient.startTransfer(participants.consumer().contextId(), participants.provider().contextId(), providerAddress, participants.provider().id(), assetId, "NonFinite-PULL");

        var consumerTransfer = connectorClient.transfers().getTransferProcess(participants.consumer().contextId(), transferProcessId);
        var providerTransfer = connectorClient.transfers().getTransferProcess(participants.provider().contextId(), consumerTransfer.getCorrelationId());

        assertThat(consumerTransfer.getState()).isEqualTo(providerTransfer.getState());


        service.complete(consumerTransfer.getId())
                .orElseThrow(f -> new EdcException(f.getFailureDetail()));

        connectorClient.waitTransferInState(participants.consumer().contextId(), transferProcessId, COMPLETED);
        connectorClient.waitTransferInState(participants.provider().contextId(), consumerTransfer.getCorrelationId(), COMPLETED);

    }

    private String setup(ManagementApiClientV5 connectorClient, Participants.Participant provider) {
        var asset = new AssetDto(Map.of());

        var permissions = List.of(new PermissionDto());
        var policyDef = new PolicyDefinitionDto(new PolicyDto(permissions));

        return connectorClient.setupResources(provider.contextId(), asset, policyDef, policyDef);

    }
}
