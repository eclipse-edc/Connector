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

import org.eclipse.edc.api.authentication.OauthServerEndToEndExtension;
import org.eclipse.edc.connector.controlplane.test.system.utils.Participants;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.ManagementApiClientV5;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.extensions.ComponentRuntimeContext;
import org.eclipse.edc.junit.extensions.ComponentRuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.nats.testfixtures.NatsEndToEndExtension;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndExtension;
import org.eclipse.edc.test.e2e.Runtimes;
import org.eclipse.edc.test.e2e.dataplane.DataPlaneSignalingClient;
import org.eclipse.edc.test.e2e.signaling.Oauth2Extension;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.HashMap;

import static org.eclipse.edc.test.e2e.TransferEndToEndTestBase.CONSUMER_DP;
import static org.eclipse.edc.test.e2e.transfer.VirtualTransferEndToEndTestBase.CONSUMER_CONTEXT;
import static org.eclipse.edc.test.e2e.transfer.VirtualTransferEndToEndTestBase.CONSUMER_ID;
import static org.eclipse.edc.test.e2e.transfer.VirtualTransferEndToEndTestBase.PROVIDER_CONTEXT;
import static org.eclipse.edc.test.e2e.transfer.VirtualTransferEndToEndTestBase.PROVIDER_ID;

class VirtualTransferEndToEndTest {


    private static Participants participants(ComponentRuntimeContext ctx) {
        var protocolEndpoint = ctx.getEndpoint("protocol");
        var signalingEndpoint = ctx.getEndpoint("signaling");
        return new Participants(
                new Participants.Participant(PROVIDER_CONTEXT, PROVIDER_ID, protocolEndpoint, signalingEndpoint),
                new Participants.Participant(CONSUMER_CONTEXT, CONSUMER_ID, protocolEndpoint, signalingEndpoint)
        );
    }

    static Config config() {
        return ConfigFactory.fromMap(new HashMap<>() {
            {
                put("edc.iam.oauth2.jwks.url", "https://example.com/jwks");
                put("edc.iam.oauth2.issuer", "test-issuer");
                put("edc.encryption.strict", "false");
                put("web.http.protocol.virtual", "true");
            }
        });
    }

    @Nested
    @PostgresqlIntegrationTest
    class PostgresNatsTest extends VirtualTransferEndToEndTestBase {

        // DPS registration
        @RegisterExtension
        @Order(0)
        static final Oauth2Extension OAUTH_SERVER = new Oauth2Extension();

        @Order(0)
        @RegisterExtension
        static final OauthServerEndToEndExtension AUTH_SERVER_EXTENSION = OauthServerEndToEndExtension.Builder.newInstance().build();

        @Order(0)
        @RegisterExtension
        static final NatsEndToEndExtension NATS_EXTENSION = new NatsEndToEndExtension();
        @Order(0)
        @RegisterExtension
        static final PostgresqlEndToEndExtension POSTGRESQL_EXTENSION = new PostgresqlEndToEndExtension();

        @Order(1)
        @RegisterExtension
        static final BeforeAllCallback SETUP = context -> {
            POSTGRESQL_EXTENSION.createDatabase(Runtimes.ControlPlane.NAME.toLowerCase());
        };

        @Order(2)
        @RegisterExtension
        static final RuntimeExtension CONTROL_PLANE = ComponentRuntimeExtension.Builder.newInstance()
                .name(Runtimes.ControlPlane.NAME)
                .modules(Runtimes.ControlPlane.VIRTUAL_MODULES)
                .modules(Runtimes.ControlPlane.IAM_MOCK)
                .modules(Runtimes.ControlPlane.SQL_MODULES)
                .modules(Runtimes.ControlPlane.VIRTUAL_SQL_MODULES)
                .modules(Runtimes.ControlPlane.VIRTUAL_NATS_MODULES)
                .endpoints(Runtimes.ControlPlane.ENDPOINTS.build())
                .configurationProvider(VirtualTransferEndToEndTest::config)
                .configurationProvider(() -> POSTGRESQL_EXTENSION.configFor(Runtimes.ControlPlane.NAME.toLowerCase()))
                .configurationProvider(NATS_EXTENSION::configFor)
                .configurationProvider(AUTH_SERVER_EXTENSION::getConfig)
                .paramProvider(ManagementApiClientV5.class, (ctx) -> ManagementApiClientV5.forContext(ctx, AUTH_SERVER_EXTENSION.getAuthServer()))
                .paramProvider(Participants.class, VirtualTransferEndToEndTest::participants)
                .build();

        @RegisterExtension
        @Order(1)
        static final RuntimeExtension PROVIDER_DATA_PLANE = ComponentRuntimeExtension.Builder.newInstance()
                .name(PROVIDER_DP)
                .modules(Runtimes.SignalingDataPlane.MODULES)
                .endpoints(Runtimes.SignalingDataPlane.ENDPOINTS.build())
                .configurationProvider(Runtimes.SignalingDataPlane::config)
                .paramProvider(DataPlaneSignalingClient.class, DataPlaneSignalingClient::new)
                .build();

        @RegisterExtension
        @Order(1)
        static final RuntimeExtension CONSUMER_DATA_PLANE = ComponentRuntimeExtension.Builder.newInstance()
                .name(CONSUMER_DP)
                .modules(Runtimes.SignalingDataPlane.MODULES)
                .endpoints(Runtimes.SignalingDataPlane.ENDPOINTS.build())
                .configurationProvider(Runtimes.SignalingDataPlane::config)
                .paramProvider(DataPlaneSignalingClient.class, DataPlaneSignalingClient::new)
                .build();

    }

}
