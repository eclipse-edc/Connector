/*
 *  Copyright (c) 2026 Think-it GmbH
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

package org.eclipse.edc.test.e2e.policy;

import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.annotations.Runtime;
import org.eclipse.edc.junit.extensions.ComponentRuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.utils.Endpoints;
import org.eclipse.edc.signaling.auth.Oauth2Extension;
import org.eclipse.edc.signaling.client.DataPlaneSignalingTestClient;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndExtension;
import org.eclipse.edc.test.e2e.Runtimes;
import org.eclipse.edc.test.e2e.TransferEndToEndParticipant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Instant;
import java.util.UUID;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static java.time.Duration.ofSeconds;
import static org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset.EDC_ASSET_TYPE_TERM;
import static org.eclipse.edc.connector.controlplane.test.system.utils.PolicyFixtures.inForceDatePolicy;
import static org.eclipse.edc.connector.controlplane.test.system.utils.PolicyFixtures.noConstraintPolicy;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTED;
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

class PolicyMonitorEndToEndTest {

    @SuppressWarnings("JUnitMalformedDeclaration")
    interface Tests {

        @BeforeAll
        static void beforeAll(@Runtime(PROVIDER_CP) TransferEndToEndParticipant provider,
                              @Runtime(CONSUMER_CP) TransferEndToEndParticipant consumer,
                              @Runtime(PROVIDER_DP) DataPlaneSignalingTestClient providerDataPlane,
                              @Runtime(CONSUMER_DP) DataPlaneSignalingTestClient consumerDataPlane,
                              Oauth2Extension oauth2) {

            var consumerDataPlaneOauth2Profile = oauth2.registerClient(consumerDataPlane.dataPlaneId());
            var consumerControlPlaneOauth2Profile = oauth2.registerClient(consumer.getId());
            var providerDataPlaneOauth2Profile = oauth2.registerClient(providerDataPlane.dataPlaneId());
            var providerControlPlaneOauth2Profile = oauth2.registerClient(provider.getId());

            consumer.registerDataPlane(consumerDataPlane.getDataPlaneRegistrationMessage(consumerControlPlaneOauth2Profile));
            provider.registerDataPlane(providerDataPlane.getDataPlaneRegistrationMessage(providerControlPlaneOauth2Profile));

            providerDataPlane.registerControlPlane(createObjectBuilder()
                    .add("controlplaneId", provider.getId())
                    .add("endpoint", provider.getSignalingEndpointUrl().toString())
                    .add("authorization", createArrayBuilder().add(providerDataPlaneOauth2Profile))
                    .build());

            consumerDataPlane.registerControlPlane(createObjectBuilder()
                    .add("controlplaneId", consumer.getId())
                    .add("endpoint", consumer.getSignalingEndpointUrl().toString())
                    .add("authorization", createArrayBuilder().add(consumerDataPlaneOauth2Profile))
                    .build());
        }

        @Test
        default void shouldTerminateTransfer_whenContractExpires_fixedInForcePeriod(
                @Runtime(PROVIDER_CP) TransferEndToEndParticipant provider,
                @Runtime(CONSUMER_CP) TransferEndToEndParticipant consumer,
                @Runtime(PROVIDER_DP) DataPlaneSignalingTestClient providerDataPlane,
                @Runtime(CONSUMER_DP) DataPlaneSignalingTestClient consumerDataPlane) {

            var now = Instant.now();
            // contract is valid from t-10s to t+10s, so it will be expired after some seconds
            var contractPolicy = inForceDatePolicy("gteq", now.minus(ofSeconds(10)), "lteq", now.plus(ofSeconds(10)));
            var contractPolicyId = provider.createPolicyDefinition(contractPolicy);
            var assetId = createOffer(provider, contractPolicyId);
            var consumerTransferProcessId = consumer.requestAssetFrom(assetId, provider)
                    .withTransferType("NonFinite-PULL").execute();

            consumer.awaitTransferToBeInState(consumerTransferProcessId, STARTED);
            var providerTransferProcessId = provider.getTransferProcessIdGivenCounterPartyOne(consumerTransferProcessId);
            consumerDataPlane.awaitFlowToBe(consumerTransferProcessId, "STARTED");
            providerDataPlane.awaitFlowToBe(providerTransferProcessId, "STARTED");

            // policy monitor should terminate the transfer

            consumer.awaitTransferToBeInState(consumerTransferProcessId, TERMINATED);
            provider.awaitTransferToBeInState(providerTransferProcessId, TERMINATED);
            consumerDataPlane.awaitFlowToBe(consumerTransferProcessId, "TERMINATED");
            providerDataPlane.awaitFlowToBe(providerTransferProcessId, "TERMINATED");
        }

        @Test
        default void shouldTerminateTransfer_whenContractExpires_durationInForcePeriod(
                @Runtime(PROVIDER_CP) TransferEndToEndParticipant provider,
                @Runtime(CONSUMER_CP) TransferEndToEndParticipant consumer,
                @Runtime(PROVIDER_DP) DataPlaneSignalingTestClient providerDataPlane,
                @Runtime(CONSUMER_DP) DataPlaneSignalingTestClient consumerDataPlane) {

            var now = Instant.now();
            // contract is valid until 5s after agreement
            var contractPolicy = inForceDatePolicy("gteq", now.minus(ofSeconds(10)), "lteq", "contractAgreement+5s");
            var contractPolicyId = provider.createPolicyDefinition(contractPolicy);
            var assetId = createOffer(provider, contractPolicyId);
            var consumerTransferProcessId = consumer.requestAssetFrom(assetId, provider)
                    .withTransferType("NonFinite-PULL").execute();

            consumer.awaitTransferToBeInState(consumerTransferProcessId, STARTED);
            var providerTransferProcessId = provider.getTransferProcessIdGivenCounterPartyOne(consumerTransferProcessId);
            consumerDataPlane.awaitFlowToBe(consumerTransferProcessId, "STARTED");
            providerDataPlane.awaitFlowToBe(providerTransferProcessId, "STARTED");

            // policy monitor should terminate the transfer

            consumer.awaitTransferToBeInState(consumerTransferProcessId, TERMINATED);
            provider.awaitTransferToBeInState(providerTransferProcessId, TERMINATED);
            consumerDataPlane.awaitFlowToBe(consumerTransferProcessId, "TERMINATED");
            providerDataPlane.awaitFlowToBe(providerTransferProcessId, "TERMINATED");
        }

        private String createOffer(TransferEndToEndParticipant provider, String contractPolicyId) {
            var createAssetRequestBody = createObjectBuilder()
                    .add(CONTEXT, createArrayBuilder().add(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2))
                    .add(TYPE, EDC_ASSET_TYPE_TERM)
                    .add("properties", createObjectBuilder()
                            .add("name", "test-asset"))
                    .build();

            var noConstraintPolicyId = provider.createPolicyDefinition(noConstraintPolicy());
            var assetId = provider.createAsset(createAssetRequestBody);
            provider.createContractDefinition(assetId, UUID.randomUUID().toString(), noConstraintPolicyId, contractPolicyId);
            return assetId;
        }
    }

    @Nested
    @EndToEndTest
    class InMemoryTest implements Tests {

        static final Endpoints CONSUMER_ENDPOINTS = Runtimes.ControlPlane.ENDPOINTS.build();
        static final Endpoints PROVIDER_ENDPOINTS = Runtimes.ControlPlane.ENDPOINTS.build();

        @RegisterExtension
        @Order(0)
        static final Oauth2Extension OAUTH_SERVER = new Oauth2Extension();

        @RegisterExtension
        @Order(0)
        static final RuntimeExtension CONSUMER_CONTROL_PLANE = ComponentRuntimeExtension.Builder.newInstance()
                .name(CONSUMER_CP)
                .modules(Runtimes.ControlPlane.CONTROL_PLANE_MODULES)
                .endpoints(CONSUMER_ENDPOINTS)
                .configurationProvider(() -> Runtimes.ControlPlane.config(CONSUMER_ID))
                .paramProvider(TransferEndToEndParticipant.class, TransferEndToEndParticipant::forContext)
                .build();

        @RegisterExtension
        @Order(0)
        static final RuntimeExtension PROVIDER_CONTROL_PLANE = ComponentRuntimeExtension.Builder.newInstance()
                .name(PROVIDER_CP)
                .modules(Runtimes.ControlPlane.CONTROL_PLANE_MODULES)
                .endpoints(PROVIDER_ENDPOINTS)
                .configurationProvider(() -> Runtimes.ControlPlane.config(PROVIDER_ID))
                .paramProvider(TransferEndToEndParticipant.class, TransferEndToEndParticipant::forContext)
                .build();

        @RegisterExtension
        @Order(1)
        static final RuntimeExtension PROVIDER_DATA_PLANE = ComponentRuntimeExtension.Builder.newInstance()
                .name(PROVIDER_DP)
                .modules(Runtimes.SignalingDataPlane.MODULES)
                .endpoints(Runtimes.SignalingDataPlane.ENDPOINTS.build())
                .configurationProvider(Runtimes.SignalingDataPlane::config)
                .paramProvider(DataPlaneSignalingTestClient.class, DataPlaneSignalingTestClient::new)
                .build();

        @RegisterExtension
        @Order(1)
        static final RuntimeExtension CONSUMER_DATA_PLANE = ComponentRuntimeExtension.Builder.newInstance()
                .name(CONSUMER_DP)
                .modules(Runtimes.SignalingDataPlane.MODULES)
                .endpoints(Runtimes.SignalingDataPlane.ENDPOINTS.build())
                .configurationProvider(Runtimes.SignalingDataPlane::config)
                .paramProvider(DataPlaneSignalingTestClient.class, DataPlaneSignalingTestClient::new)
                .build();
    }

    @Nested
    @PostgresqlIntegrationTest
    class PostgresTest implements Tests {

        static final String CONSUMER_DB = "consumer";
        static final String PROVIDER_DB = "provider";

        @RegisterExtension
        @Order(0)
        static final Oauth2Extension OAUTH_SERVER = new Oauth2Extension();

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
                .modules(Runtimes.ControlPlane.CONTROL_PLANE_MODULES)
                .modules(Runtimes.ControlPlane.SQL_MODULES)
                .endpoints(CONSUMER_ENDPOINTS)
                .configurationProvider(() -> Runtimes.ControlPlane.config(CONSUMER_ID))
                .configurationProvider(() -> POSTGRESQL_EXTENSION.configFor(CONSUMER_DB))
                .paramProvider(TransferEndToEndParticipant.class, TransferEndToEndParticipant::forContext)
                .build();

        @RegisterExtension
        @Order(2)
        static final RuntimeExtension PROVIDER_CONTROL_PLANE = ComponentRuntimeExtension.Builder.newInstance()
                .name(PROVIDER_CP)
                .modules(Runtimes.ControlPlane.CONTROL_PLANE_MODULES)
                .modules(Runtimes.ControlPlane.SQL_MODULES)
                .endpoints(PROVIDER_ENDPOINTS)
                .configurationProvider(() -> Runtimes.ControlPlane.config(PROVIDER_ID))
                .configurationProvider(() -> POSTGRESQL_EXTENSION.configFor(PROVIDER_DB))
                .paramProvider(TransferEndToEndParticipant.class, TransferEndToEndParticipant::forContext)
                .build();

        @RegisterExtension
        @Order(3)
        static final RuntimeExtension CONSUMER_DATA_PLANE = ComponentRuntimeExtension.Builder.newInstance()
                .name(CONSUMER_DP)
                .modules(Runtimes.SignalingDataPlane.MODULES)
                .endpoints(Runtimes.SignalingDataPlane.ENDPOINTS.build())
                .configurationProvider(Runtimes.SignalingDataPlane::config)
                .paramProvider(DataPlaneSignalingTestClient.class, DataPlaneSignalingTestClient::new)
                .build();

        @RegisterExtension
        @Order(3)
        static final RuntimeExtension PROVIDER_DATA_PLANE = ComponentRuntimeExtension.Builder.newInstance()
                .name(PROVIDER_DP)
                .modules(Runtimes.SignalingDataPlane.MODULES)
                .endpoints(Runtimes.SignalingDataPlane.ENDPOINTS.build())
                .configurationProvider(Runtimes.SignalingDataPlane::config)
                .paramProvider(DataPlaneSignalingTestClient.class, DataPlaneSignalingTestClient::new)
                .build();
    }

}
