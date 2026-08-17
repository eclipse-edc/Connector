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

package org.eclipse.edc.test.e2e.signaling;

import jakarta.json.JsonObject;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.Runtime;
import org.eclipse.edc.junit.extensions.ComponentRuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.utils.Endpoints;
import org.eclipse.edc.signaling.auth.TokenExchangeExtension;
import org.eclipse.edc.signaling.client.DataPlaneSignalingTestClient;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.test.e2e.Runtimes;
import org.eclipse.edc.test.e2e.TransferEndToEndParticipant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.UUID;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset.EDC_ASSET_TYPE_TERM;
import static org.eclipse.edc.connector.controlplane.test.system.utils.PolicyFixtures.noConstraintPolicy;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.COMPLETED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.TERMINATED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.signaling.auth.TokenExchangeExtension.AUDIENCE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2;
import static org.eclipse.edc.test.e2e.TransferEndToEndTestBase.CONSUMER_CP;
import static org.eclipse.edc.test.e2e.TransferEndToEndTestBase.CONSUMER_DP;
import static org.eclipse.edc.test.e2e.TransferEndToEndTestBase.CONSUMER_ID;
import static org.eclipse.edc.test.e2e.TransferEndToEndTestBase.PROVIDER_CP;
import static org.eclipse.edc.test.e2e.TransferEndToEndTestBase.PROVIDER_DP;
import static org.eclipse.edc.test.e2e.TransferEndToEndTestBase.PROVIDER_ID;

/**
 * Exercises the {@code oauth2_token_exchange} Data Plane Signaling authorization profile in both directions: the
 * control plane exchanges its workload credential for a scoped JWT before calling the data plane, and the data plane
 * does the same before notifying the control plane. Tokens are signed by the stub broker and verified against its
 * JWKS endpoint on both ends.
 */
@EndToEndTest
class TokenExchangeSignalingEndToEndTest {

    static final Endpoints CONSUMER_ENDPOINTS = Runtimes.ControlPlane.ENDPOINTS.build();
    static final Endpoints PROVIDER_ENDPOINTS = Runtimes.ControlPlane.ENDPOINTS.build();
    @RegisterExtension
    @Order(0)
    static final TokenExchangeExtension BROKER = new TokenExchangeExtension();
    @RegisterExtension
    @Order(1)
    static final RuntimeExtension CONSUMER_CONTROL_PLANE = ComponentRuntimeExtension.Builder.newInstance()
            .name(CONSUMER_CP)
            .modules(Runtimes.ControlPlane.CONTROL_PLANE_MODULES)
            .endpoints(CONSUMER_ENDPOINTS)
            .configurationProvider(() -> Runtimes.ControlPlane.config(CONSUMER_ID))
            .configurationProvider(TokenExchangeSignalingEndToEndTest::tokenExchangeConfig)
            .paramProvider(TransferEndToEndParticipant.class, TransferEndToEndParticipant::forContext)
            .build();
    @RegisterExtension
    @Order(1)
    static final RuntimeExtension PROVIDER_CONTROL_PLANE = ComponentRuntimeExtension.Builder.newInstance()
            .name(PROVIDER_CP)
            .modules(Runtimes.ControlPlane.CONTROL_PLANE_MODULES)
            .endpoints(PROVIDER_ENDPOINTS)
            .configurationProvider(() -> Runtimes.ControlPlane.config(PROVIDER_ID))
            .configurationProvider(TokenExchangeSignalingEndToEndTest::tokenExchangeConfig)
            .paramProvider(TransferEndToEndParticipant.class, TransferEndToEndParticipant::forContext)
            .build();
    @RegisterExtension
    @Order(2)
    static final RuntimeExtension PROVIDER_DATA_PLANE = ComponentRuntimeExtension.Builder.newInstance()
            .name(PROVIDER_DP)
            .modules(Runtimes.SignalingDataPlane.MODULES)
            .endpoints(Runtimes.SignalingDataPlane.ENDPOINTS.build())
            .configurationProvider(Runtimes.SignalingDataPlane::config)
            .configurationProvider(TokenExchangeSignalingEndToEndTest::dataPlaneAuthorizationConfig)
            .paramProvider(DataPlaneSignalingTestClient.class, DataPlaneSignalingTestClient::new)
            .build();
    @RegisterExtension
    @Order(2)
    static final RuntimeExtension CONSUMER_DATA_PLANE = ComponentRuntimeExtension.Builder.newInstance()
            .name(CONSUMER_DP)
            .modules(Runtimes.SignalingDataPlane.MODULES)
            .endpoints(Runtimes.SignalingDataPlane.ENDPOINTS.build())
            .configurationProvider(Runtimes.SignalingDataPlane::config)
            .configurationProvider(TokenExchangeSignalingEndToEndTest::dataPlaneAuthorizationConfig)
            .paramProvider(DataPlaneSignalingTestClient.class, DataPlaneSignalingTestClient::new)
            .build();
    private static final String WORKLOAD_CREDENTIAL_PATH = createWorkloadCredentialFile();

    private static Config dataPlaneAuthorizationConfig() {
        return ConfigFactory.fromMap(Map.of("dataplane.authorization", "oauth2_token_exchange"));
    }

    private static Config tokenExchangeConfig() {
        return ConfigFactory.fromMap(Map.of(
                "edc.dps.oauth2.tokenexchange.subjecttokenpath", WORKLOAD_CREDENTIAL_PATH,
                "edc.dps.oauth2.tokenexchange.audience", AUDIENCE
        ));
    }

    private static String createWorkloadCredentialFile() {
        try {
            var file = Files.createTempFile("dps-workload-credential", ".jwt");
            Files.writeString(file, "a-projected-service-account-token");
            file.toFile().deleteOnExit();
            return file.toString();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @BeforeAll
    static void beforeAll(@Runtime(PROVIDER_CP) TransferEndToEndParticipant provider,
                          @Runtime(CONSUMER_CP) TransferEndToEndParticipant consumer,
                          @Runtime(PROVIDER_DP) DataPlaneSignalingTestClient providerDataPlane,
                          @Runtime(CONSUMER_DP) DataPlaneSignalingTestClient consumerDataPlane,
                          TokenExchangeExtension broker) {

        // the control planes speak for themselves towards their data plane. The consumer profile omits the
        // 'audience' property, so that control plane has to fall back to its configured default audience, whereas
        // the provider profile carries it explicitly: both paths must resolve to the audience the broker expects.
        var consumerControlPlaneProfile = broker.registerPrincipalWithoutAudience(consumer.getId());
        var providerControlPlaneProfile = broker.registerPrincipal(provider.getId());

        // the data planes speak for themselves towards their control plane. Their profiles always carry the
        // audience: the stub data plane reads it straight off the profile and has no EDC configuration.
        var consumerDataPlaneProfile = broker.registerPrincipal(consumerDataPlane.dataPlaneId());
        var providerDataPlaneProfile = broker.registerPrincipal(providerDataPlane.dataPlaneId());

        consumer.registerDataPlane(consumerDataPlane.getDataPlaneRegistrationMessage(consumerControlPlaneProfile));
        provider.registerDataPlane(providerDataPlane.getDataPlaneRegistrationMessage(providerControlPlaneProfile));

        providerDataPlane.registerControlPlane(createObjectBuilder()
                .add("controlplaneId", provider.getId())
                .add("endpoint", provider.getSignalingEndpointUrl().toString())
                .add("authorization", withWorkloadCredential(providerDataPlaneProfile))
                .build());

        consumerDataPlane.registerControlPlane(createObjectBuilder()
                .add("controlplaneId", consumer.getId())
                .add("endpoint", consumer.getSignalingEndpointUrl().toString())
                .add("authorization", withWorkloadCredential(consumerDataPlaneProfile))
                .build());
    }

    /**
     * The stub data plane reads its workload credential from a path carried by the profile, whereas the control plane
     * takes it from its own configuration.
     */
    private static JsonObject withWorkloadCredential(JsonObject profile) {
        return createObjectBuilder(profile).add("subjectTokenPath", WORKLOAD_CREDENTIAL_PATH).build();
    }

    @Test
    void shouldTransferFiniteDataWithPush(@Runtime(PROVIDER_CP) TransferEndToEndParticipant provider,
                                          @Runtime(CONSUMER_CP) TransferEndToEndParticipant consumer,
                                          @Runtime(PROVIDER_DP) DataPlaneSignalingTestClient providerDataPlane,
                                          @Runtime(CONSUMER_DP) DataPlaneSignalingTestClient consumerDataPlane,
                                          TokenExchangeExtension broker) {

        var assetId = createOffer(provider);
        var consumerTransferProcessId = consumer.requestAssetFrom(assetId, provider)
                .withTransferType("Finite-PUSH").execute();

        consumer.awaitTransferToBeInState(consumerTransferProcessId, COMPLETED);
        var providerTransferProcessId = provider.getTransferProcessIdGivenCounterPartyOne(consumerTransferProcessId);
        consumerDataPlane.awaitFlowToBe(consumerTransferProcessId, "COMPLETED");
        providerDataPlane.awaitFlowToBe(providerTransferProcessId, "COMPLETED");

        // exchanged tokens are not cached: every signaling message triggers a fresh exchange
        assertThat(broker.exchangeCount(consumer.getId())).isPositive();
        assertThat(broker.exchangeCount(provider.getId())).isPositive();
        // the pushing data plane exchanged too, and the control plane verified the resulting signature against the broker JWKS
        assertThat(broker.exchangeCount(providerDataPlane.dataPlaneId())).isPositive();
        assertThat(broker.jwksRequestCount()).isPositive();
    }

    @Test
    void shouldTransferNonFiniteDataWithPull(@Runtime(PROVIDER_CP) TransferEndToEndParticipant provider,
                                             @Runtime(CONSUMER_CP) TransferEndToEndParticipant consumer,
                                             @Runtime(PROVIDER_DP) DataPlaneSignalingTestClient providerDataPlane,
                                             @Runtime(CONSUMER_DP) DataPlaneSignalingTestClient consumerDataPlane) {

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
}
