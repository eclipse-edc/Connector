/*
 *  Copyright (c) 2025 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.test.e2e.signaling;

import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.annotations.Runtime;
import org.eclipse.edc.junit.extensions.ComponentRuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.utils.Endpoints;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndExtension;
import org.eclipse.edc.test.e2e.Runtimes;
import org.eclipse.edc.test.e2e.TransferEndToEndParticipant;
import org.eclipse.edc.test.e2e.dataplane.DataPlaneSignalingClient;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.UUID;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset.EDC_ASSET_TYPE_TERM;
import static org.eclipse.edc.connector.controlplane.test.system.utils.Participant.MANAGEMENT_V4;
import static org.eclipse.edc.connector.controlplane.test.system.utils.PolicyFixtures.noConstraintPolicy;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.COMPLETED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.PREPARATION_REQUESTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.REQUESTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTUP_REQUESTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.SUSPENDED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.TERMINATED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2;
import static org.eclipse.edc.test.e2e.TransferEndToEndTestBase.CONSUMER_CP;
import static org.eclipse.edc.test.e2e.TransferEndToEndTestBase.CONSUMER_DP;
import static org.eclipse.edc.test.e2e.TransferEndToEndTestBase.CONSUMER_ID;
import static org.eclipse.edc.test.e2e.TransferEndToEndTestBase.PROVIDER_CP;
import static org.eclipse.edc.test.e2e.TransferEndToEndTestBase.PROVIDER_DP;
import static org.eclipse.edc.test.e2e.TransferEndToEndTestBase.PROVIDER_ID;


interface TransferSignalingEndToEndTest {

    @Test
    default void shouldTransferFiniteDataWithPush(@Runtime(PROVIDER_CP) TransferEndToEndParticipant provider,
                                                  @Runtime(CONSUMER_CP) TransferEndToEndParticipant consumer,
                                                  @Runtime(PROVIDER_DP) DataPlaneSignalingClient providerDataPlane,
                                                  @Runtime(CONSUMER_DP) DataPlaneSignalingClient consumerDataPlane) {

        var assetId = createOffer(provider);
        var consumerTransferProcessId = consumer.requestAssetFrom(assetId, provider)
                .withTransferType("Finite-PUSH").execute();

        consumer.awaitTransferToBeInState(consumerTransferProcessId, COMPLETED);
        var providerTransferProcessId = provider.getTransferProcessIdGivenCounterPartyOne(consumerTransferProcessId);
        consumerDataPlane.awaitFlowToBe(consumerTransferProcessId, "COMPLETED");
        providerDataPlane.awaitFlowToBe(providerTransferProcessId, "COMPLETED");
    }

    @Test
    default void shouldTransferNonFiniteDataWithPush(@Runtime(PROVIDER_CP) TransferEndToEndParticipant provider,
                                                     @Runtime(CONSUMER_CP) TransferEndToEndParticipant consumer,
                                                     @Runtime(PROVIDER_DP) DataPlaneSignalingClient providerDataPlane,
                                                     @Runtime(CONSUMER_DP) DataPlaneSignalingClient consumerDataPlane) {

        var assetId = createOffer(provider);
        var consumerTransferProcessId = consumer.requestAssetFrom(assetId, provider)
                .withTransferType("NonFinite-PUSH").execute();

        consumer.awaitTransferToBeInState(consumerTransferProcessId, STARTED);
        var providerTransferProcessId = provider.getTransferProcessIdGivenCounterPartyOne(consumerTransferProcessId);
        consumerDataPlane.awaitFlowToBe(consumerTransferProcessId, "STARTED");
        providerDataPlane.awaitFlowToBe(providerTransferProcessId, "STARTED");

        provider.terminateTransfer(providerTransferProcessId);
        provider.awaitTransferToBeInState(providerTransferProcessId, TERMINATED);
        consumer.awaitTransferToBeInState(consumerTransferProcessId, TERMINATED);
        consumerDataPlane.awaitFlowToBe(consumerTransferProcessId, "TERMINATED");
        providerDataPlane.awaitFlowToBe(providerTransferProcessId, "TERMINATED");
    }

    @Test
    default void shouldTransferNonFiniteDataWithPull(@Runtime(PROVIDER_CP) TransferEndToEndParticipant provider,
                                                     @Runtime(CONSUMER_CP) TransferEndToEndParticipant consumer,
                                                     @Runtime(PROVIDER_DP) DataPlaneSignalingClient providerDataPlane,
                                                     @Runtime(CONSUMER_DP) DataPlaneSignalingClient consumerDataPlane) {

        var assetId = createOffer(provider);
        var consumerTransferProcessId = consumer.requestAssetFrom(assetId, provider)
                .withTransferType("NonFinite-PULL").execute();

        consumer.awaitTransferToBeInState(consumerTransferProcessId, STARTED);
        var providerTransferProcessId = provider.getTransferProcessIdGivenCounterPartyOne(consumerTransferProcessId);
        consumerDataPlane.awaitFlowToBe(consumerTransferProcessId, "STARTED");
        providerDataPlane.awaitFlowToBe(providerTransferProcessId, "STARTED");

        consumer.terminateTransfer(consumerTransferProcessId);
        consumer.awaitTransferToBeInState(consumerTransferProcessId, TERMINATED);
        provider.awaitTransferToBeInState(providerTransferProcessId, TERMINATED);
        consumerDataPlane.awaitFlowToBe(consumerTransferProcessId, "TERMINATED");
        providerDataPlane.awaitFlowToBe(providerTransferProcessId, "TERMINATED");
    }

    @Test
    default void shouldTransferFiniteDataWithPull(@Runtime(PROVIDER_CP) TransferEndToEndParticipant provider,
                                                  @Runtime(CONSUMER_CP) TransferEndToEndParticipant consumer,
                                                  @Runtime(PROVIDER_DP) DataPlaneSignalingClient providerDataPlane,
                                                  @Runtime(CONSUMER_DP) DataPlaneSignalingClient consumerDataPlane) {

        var assetId = createOffer(provider);
        var consumerTransferProcessId = consumer.requestAssetFrom(assetId, provider)
                .withTransferType("Finite-PULL").execute();

        consumer.awaitTransferToBeInState(consumerTransferProcessId, COMPLETED);
        var providerTransferProcessId = provider.getTransferProcessIdGivenCounterPartyOne(consumerTransferProcessId);
        provider.awaitTransferToBeInState(providerTransferProcessId, COMPLETED);
        consumerDataPlane.awaitFlowToBe(consumerTransferProcessId, "COMPLETED");
        providerDataPlane.awaitFlowToBe(providerTransferProcessId, "COMPLETED");
    }

    @Test
    default void shouldSuspendAndResumeFromProvider(@Runtime(PROVIDER_CP) TransferEndToEndParticipant provider,
                                                    @Runtime(CONSUMER_CP) TransferEndToEndParticipant consumer,
                                                    @Runtime(PROVIDER_DP) DataPlaneSignalingClient providerDataPlane,
                                                    @Runtime(CONSUMER_DP) DataPlaneSignalingClient consumerDataPlane) {

        var assetId = createOffer(provider);
        var consumerTransferProcessId = consumer.requestAssetFrom(assetId, provider)
                .withTransferType("NonFinite-PULL").execute();

        consumer.awaitTransferToBeInState(consumerTransferProcessId, STARTED);
        var providerTransferProcessId = provider.getTransferProcessIdGivenCounterPartyOne(consumerTransferProcessId);
        provider.awaitTransferToBeInState(providerTransferProcessId, STARTED);
        consumerDataPlane.awaitFlowToBe(consumerTransferProcessId, "STARTED");
        providerDataPlane.awaitFlowToBe(providerTransferProcessId, "STARTED");

        provider.suspendTransfer(providerTransferProcessId, "any reason");

        provider.awaitTransferToBeInState(providerTransferProcessId, SUSPENDED);
        consumer.awaitTransferToBeInState(consumerTransferProcessId, SUSPENDED);
        consumerDataPlane.awaitFlowToBe(consumerTransferProcessId, "SUSPENDED");
        providerDataPlane.awaitFlowToBe(providerTransferProcessId, "SUSPENDED");

        provider.resumeTransfer(providerTransferProcessId);

        provider.awaitTransferToBeInState(providerTransferProcessId, STARTED);
        consumer.awaitTransferToBeInState(consumerTransferProcessId, STARTED);
        consumerDataPlane.awaitFlowToBe(consumerTransferProcessId, "STARTED");
        providerDataPlane.awaitFlowToBe(providerTransferProcessId, "STARTED");
    }

    @Test
    default void shouldSuspendAndResumeFromConsumer(@Runtime(PROVIDER_CP) TransferEndToEndParticipant provider,
                                                    @Runtime(CONSUMER_CP) TransferEndToEndParticipant consumer,
                                                    @Runtime(PROVIDER_DP) DataPlaneSignalingClient providerDataPlane,
                                                    @Runtime(CONSUMER_DP) DataPlaneSignalingClient consumerDataPlane) {

        var assetId = createOffer(provider);
        var consumerTransferProcessId = consumer.requestAssetFrom(assetId, provider)
                .withTransferType("NonFinite-PUSH").execute();

        consumer.awaitTransferToBeInState(consumerTransferProcessId, STARTED);
        var providerTransferProcessId = provider.getTransferProcessIdGivenCounterPartyOne(consumerTransferProcessId);
        provider.awaitTransferToBeInState(providerTransferProcessId, STARTED);
        consumerDataPlane.awaitFlowToBe(consumerTransferProcessId, "STARTED");
        providerDataPlane.awaitFlowToBe(providerTransferProcessId, "STARTED");

        consumer.suspendTransfer(consumerTransferProcessId, "any reason");

        consumer.awaitTransferToBeInState(consumerTransferProcessId, SUSPENDED);
        provider.awaitTransferToBeInState(providerTransferProcessId, SUSPENDED);
        consumerDataPlane.awaitFlowToBe(consumerTransferProcessId, "SUSPENDED");
        providerDataPlane.awaitFlowToBe(providerTransferProcessId, "SUSPENDED");

        consumer.resumeTransfer(consumerTransferProcessId);

        consumer.awaitTransferToBeInState(consumerTransferProcessId, STARTED);
        provider.awaitTransferToBeInState(providerTransferProcessId, STARTED);
        consumerDataPlane.awaitFlowToBe(consumerTransferProcessId, "STARTED");
        providerDataPlane.awaitFlowToBe(providerTransferProcessId, "STARTED");
    }

    @Test
    default void shouldSupportAsyncPreparation(@Runtime(PROVIDER_CP) TransferEndToEndParticipant provider,
                                               @Runtime(CONSUMER_CP) TransferEndToEndParticipant consumer,
                                               @Runtime(PROVIDER_DP) DataPlaneSignalingClient providerDataPlane,
                                               @Runtime(CONSUMER_DP) DataPlaneSignalingClient consumerDataPlane) {
        var assetId = createOffer(provider);
        var consumerTransferProcessId = consumer.requestAssetFrom(assetId, provider)
                .withTransferType("AsyncPrepare-PUSH").execute();

        consumer.awaitTransferToBeInState(consumerTransferProcessId, PREPARATION_REQUESTED);
        consumerDataPlane.awaitFlowToBe(consumerTransferProcessId, "PREPARING");

        consumerDataPlane.completePreparation(consumerTransferProcessId);

        consumer.awaitTransferToBeInState(consumerTransferProcessId, COMPLETED);
        var providerTransferProcessId = provider.getTransferProcessIdGivenCounterPartyOne(consumerTransferProcessId);
        consumerDataPlane.awaitFlowToBe(consumerTransferProcessId, "COMPLETED");
        providerDataPlane.awaitFlowToBe(providerTransferProcessId, "COMPLETED");
    }

    @Test
    default void shouldSupportAsyncStartup(@Runtime(PROVIDER_CP) TransferEndToEndParticipant provider,
                                   @Runtime(CONSUMER_CP) TransferEndToEndParticipant consumer,
                                   @Runtime(PROVIDER_DP) DataPlaneSignalingClient providerDataPlane,
                                   @Runtime(CONSUMER_DP) DataPlaneSignalingClient consumerDataPlane) {
        var assetId = createOffer(provider);
        var consumerTransferProcessId = consumer.requestAssetFrom(assetId, provider)
                .withTransferType("AsyncStart-PULL").execute();

        consumer.awaitTransferToBeInState(consumerTransferProcessId, REQUESTED);
        var providerTransferProcessId = provider.getTransferProcessIdGivenCounterPartyOne(consumerTransferProcessId);

        provider.awaitTransferToBeInState(providerTransferProcessId, STARTUP_REQUESTED);
        providerDataPlane.awaitFlowToBe(providerTransferProcessId, "STARTING");

        providerDataPlane.completeStartup(providerTransferProcessId);

        consumer.awaitTransferToBeInState(consumerTransferProcessId, COMPLETED);
        provider.awaitTransferToBeInState(providerTransferProcessId, COMPLETED);
        consumerDataPlane.awaitFlowToBe(consumerTransferProcessId, "COMPLETED");
        providerDataPlane.awaitFlowToBe(providerTransferProcessId, "COMPLETED");
    }

    private String createOffer(TransferEndToEndParticipant provider) {
        var createAssetRequestBody = createObjectBuilder()
                .add(CONTEXT, createArrayBuilder().add(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2))
                .add(TYPE, EDC_ASSET_TYPE_TERM)
                .add("properties", createObjectBuilder()
                        .add("name", "test-asset"))
                .build();

        var noConstraintPolicyId = provider.createPolicyDefinition(noConstraintPolicy());
        var assetId = provider.createAsset(createAssetRequestBody);
        provider.createContractDefinition(assetId, UUID.randomUUID().toString(), noConstraintPolicyId, noConstraintPolicyId);
        return assetId;
    }

    @Nested
    @EndToEndTest
    class InMemoryTest implements TransferSignalingEndToEndTest {

        static final Endpoints CONSUMER_ENDPOINTS = Runtimes.ControlPlane.ENDPOINTS.build();
        static final Endpoints PROVIDER_ENDPOINTS = Runtimes.ControlPlane.ENDPOINTS.build();

        @RegisterExtension
        @Order(0)
        static final RuntimeExtension CONSUMER_CONTROL_PLANE = ComponentRuntimeExtension.Builder.newInstance()
                .name(CONSUMER_CP)
                .modules(Runtimes.ControlPlane.SIGNALING_MODULES)
                .endpoints(CONSUMER_ENDPOINTS)
                .configurationProvider(() -> Runtimes.ControlPlane.config(CONSUMER_ID))
                .paramProvider(TransferEndToEndParticipant.class, ctx -> TransferEndToEndParticipant.newInstance(ctx)
                        .managementVersionBasePath(MANAGEMENT_V4)
                        .build())
                .build();

        @RegisterExtension
        @Order(0)
        static final RuntimeExtension PROVIDER_CONTROL_PLANE = ComponentRuntimeExtension.Builder.newInstance()
                .name(PROVIDER_CP)
                .modules(Runtimes.ControlPlane.SIGNALING_MODULES)
                .endpoints(PROVIDER_ENDPOINTS)
                .configurationProvider(() -> Runtimes.ControlPlane.config(PROVIDER_ID))
                .paramProvider(TransferEndToEndParticipant.class, ctx -> TransferEndToEndParticipant.newInstance(ctx)
                        .managementVersionBasePath(MANAGEMENT_V4)
                        .build())
                .build();

        @RegisterExtension
        @Order(1)
        static final RuntimeExtension PROVIDER_DATA_PLANE = ComponentRuntimeExtension.Builder.newInstance()
                .name(PROVIDER_DP)
                .modules(Runtimes.SignalingDataPlane.MODULES)
                .endpoints(Runtimes.SignalingDataPlane.ENDPOINTS.build())
                .configurationProvider(Runtimes.SignalingDataPlane::config)
                .configurationProvider(() -> Runtimes.ControlPlane.controlPlaneEndpointOf(PROVIDER_ENDPOINTS))
                .paramProvider(DataPlaneSignalingClient.class, DataPlaneSignalingClient::new)
                .build();

        @RegisterExtension
        @Order(1)
        static final RuntimeExtension CONSUMER_DATA_PLANE = ComponentRuntimeExtension.Builder.newInstance()
                .name(CONSUMER_DP)
                .modules(Runtimes.SignalingDataPlane.MODULES)
                .endpoints(Runtimes.SignalingDataPlane.ENDPOINTS.build())
                .configurationProvider(Runtimes.SignalingDataPlane::config)
                .configurationProvider(() -> Runtimes.ControlPlane.controlPlaneEndpointOf(CONSUMER_ENDPOINTS))
                .paramProvider(DataPlaneSignalingClient.class, DataPlaneSignalingClient::new)
                .build();
    }

    @Nested
    @PostgresqlIntegrationTest
    class PostgresTest implements TransferSignalingEndToEndTest {

        static final String CONSUMER_DB = "consumer";
        static final String PROVIDER_DB = "provider";

        @Order(0)
        @RegisterExtension
        static final PostgresqlEndToEndExtension POSTGRESQL_EXTENSION = new PostgresqlEndToEndExtension();

        @Order(1)
        @RegisterExtension
        static final BeforeAllCallback CREATE_DATABASES = context -> {
            POSTGRESQL_EXTENSION.createDatabase(CONSUMER_DB);
            POSTGRESQL_EXTENSION.createDatabase(PROVIDER_DB);
        };

        static final Endpoints CONSUMER_ENDPOINTS = Runtimes.ControlPlane.ENDPOINTS.build();
        static final Endpoints PROVIDER_ENDPOINTS = Runtimes.ControlPlane.ENDPOINTS.build();

        @RegisterExtension
        @Order(2)
        static final RuntimeExtension CONSUMER_CONTROL_PLANE = ComponentRuntimeExtension.Builder.newInstance()
                .name(CONSUMER_CP)
                .modules(Runtimes.ControlPlane.SIGNALING_MODULES)
                .modules(Runtimes.ControlPlane.SQL_MODULES)
                .endpoints(CONSUMER_ENDPOINTS)
                .configurationProvider(() -> Runtimes.ControlPlane.config(CONSUMER_ID))
                .configurationProvider(() -> POSTGRESQL_EXTENSION.configFor(CONSUMER_DB))
                .paramProvider(TransferEndToEndParticipant.class, ctx -> TransferEndToEndParticipant.newInstance(ctx)
                        .managementVersionBasePath(MANAGEMENT_V4)
                        .build())
                .build();

        @RegisterExtension
        @Order(2)
        static final RuntimeExtension PROVIDER_CONTROL_PLANE = ComponentRuntimeExtension.Builder.newInstance()
                .name(PROVIDER_CP)
                .modules(Runtimes.ControlPlane.SIGNALING_MODULES)
                .modules(Runtimes.ControlPlane.SQL_MODULES)
                .endpoints(PROVIDER_ENDPOINTS)
                .configurationProvider(() -> Runtimes.ControlPlane.config(PROVIDER_ID))
                .configurationProvider(() -> POSTGRESQL_EXTENSION.configFor(PROVIDER_DB))
                .paramProvider(TransferEndToEndParticipant.class, ctx -> TransferEndToEndParticipant.newInstance(ctx)
                        .managementVersionBasePath(MANAGEMENT_V4)
                        .build())
                .build();

        @RegisterExtension
        @Order(3)
        static final RuntimeExtension CONSUMER_DATA_PLANE = ComponentRuntimeExtension.Builder.newInstance()
                .name(CONSUMER_DP)
                .modules(Runtimes.SignalingDataPlane.MODULES)
                .endpoints(Runtimes.SignalingDataPlane.ENDPOINTS.build())
                .configurationProvider(Runtimes.SignalingDataPlane::config)
                .configurationProvider(() -> Runtimes.ControlPlane.controlPlaneEndpointOf(CONSUMER_ENDPOINTS))
                .paramProvider(DataPlaneSignalingClient.class, DataPlaneSignalingClient::new)
                .build();

        @RegisterExtension
        @Order(3)
        static final RuntimeExtension PROVIDER_DATA_PLANE = ComponentRuntimeExtension.Builder.newInstance()
                .name(PROVIDER_DP)
                .modules(Runtimes.SignalingDataPlane.MODULES)
                .endpoints(Runtimes.SignalingDataPlane.ENDPOINTS.build())
                .configurationProvider(Runtimes.SignalingDataPlane::config)
                .configurationProvider(() -> Runtimes.ControlPlane.controlPlaneEndpointOf(PROVIDER_ENDPOINTS))
                .paramProvider(DataPlaneSignalingClient.class, DataPlaneSignalingClient::new)
                .build();
    }

}
