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
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2;
import static org.eclipse.edc.test.e2e.TransferEndToEndTestBase.CONSUMER_CP;
import static org.eclipse.edc.test.e2e.TransferEndToEndTestBase.CONSUMER_ID;
import static org.eclipse.edc.test.e2e.TransferEndToEndTestBase.PROVIDER_CP;
import static org.eclipse.edc.test.e2e.TransferEndToEndTestBase.PROVIDER_DP;
import static org.eclipse.edc.test.e2e.TransferEndToEndTestBase.PROVIDER_ID;
import static org.hamcrest.Matchers.greaterThan;


interface TransferPushSignalingTest {

    @Test
    default void shouldRegisterDataPlaneThroughSignaling(@Runtime(PROVIDER_CP) TransferEndToEndParticipant provider,
                                                         @Runtime(CONSUMER_CP) TransferEndToEndParticipant consumer) {
        provider.baseManagementRequest()
                .get("/dataplanes")
                .then()
                .body("size()", greaterThan(0));

        var createAssetRequestBody = createObjectBuilder()
                .add(CONTEXT, createArrayBuilder().add(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2))
                .add(TYPE, EDC_ASSET_TYPE_TERM)
                .add("properties", createObjectBuilder()
                        .add("name", "test-asset"))
                .build();

        var noConstraintPolicyId = provider.createPolicyDefinition(noConstraintPolicy());
        var assetId = provider.createAsset(createAssetRequestBody);
        provider.createContractDefinition(assetId, UUID.randomUUID().toString(), noConstraintPolicyId, noConstraintPolicyId);

        var transferProcessId = consumer.requestAssetFrom(assetId, provider)
                .withTransferType("Finite-PUSH").execute();

        consumer.awaitTransferToBeInState(transferProcessId, STARTED);
    }

    @Nested
    @EndToEndTest
    class InMemory implements TransferPushSignalingTest {

        @RegisterExtension
        static final RuntimeExtension CONSUMER_CONTROL_PLANE = ComponentRuntimeExtension.Builder.newInstance()
                .name(CONSUMER_CP)
                .modules(Runtimes.ControlPlane.SIGNALING_MODULES)
                .endpoints(Runtimes.ControlPlane.ENDPOINTS.build())
                .configurationProvider(() -> Runtimes.ControlPlane.config(CONSUMER_ID))
                .paramProvider(TransferEndToEndParticipant.class, ctx -> TransferEndToEndParticipant.newInstance(ctx)
                        .managementVersionBasePath(MANAGEMENT_V4)
                        .build())
                .build();

        static final Endpoints PROVIDER_ENDPOINTS = Runtimes.ControlPlane.ENDPOINTS.build();

        @RegisterExtension
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
        static final RuntimeExtension PROVIDER_DATA_PLANE = ComponentRuntimeExtension.Builder.newInstance()
                .name(PROVIDER_DP)
                .modules(Runtimes.SignalingDataPlane.MODULES)
                .endpoints(Runtimes.SignalingDataPlane.ENDPOINTS.build())
                .configurationProvider(Runtimes.SignalingDataPlane::config)
                .configurationProvider(() -> Runtimes.ControlPlane.controlPlaneEndpointOf(PROVIDER_ENDPOINTS))
                .build();
    }

    @Nested
    @PostgresqlIntegrationTest
    class Postgres implements TransferPushSignalingTest {

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

        @RegisterExtension
        static final RuntimeExtension CONSUMER_CONTROL_PLANE = ComponentRuntimeExtension.Builder.newInstance()
                .name(CONSUMER_CP)
                .modules(Runtimes.ControlPlane.SIGNALING_MODULES)
                .modules(Runtimes.ControlPlane.SQL_MODULES)
                .endpoints(Runtimes.ControlPlane.ENDPOINTS.build())
                .configurationProvider(() -> Runtimes.ControlPlane.config(CONSUMER_ID))
                .configurationProvider(() -> POSTGRESQL_EXTENSION.configFor(CONSUMER_DB))
                .paramProvider(TransferEndToEndParticipant.class, ctx -> TransferEndToEndParticipant.newInstance(ctx)
                        .managementVersionBasePath(MANAGEMENT_V4)
                        .build())
                .build();

        static final Endpoints PROVIDER_ENDPOINTS = Runtimes.ControlPlane.ENDPOINTS.build();

        @RegisterExtension
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
        static final RuntimeExtension PROVIDER_DATA_PLANE = ComponentRuntimeExtension.Builder.newInstance()
                .name(PROVIDER_DP)
                .modules(Runtimes.SignalingDataPlane.MODULES)
                .endpoints(Runtimes.SignalingDataPlane.ENDPOINTS.build())
                .configurationProvider(Runtimes.SignalingDataPlane::config)
                .configurationProvider(() -> Runtimes.ControlPlane.controlPlaneEndpointOf(PROVIDER_ENDPOINTS))
                .build();
    }

}
