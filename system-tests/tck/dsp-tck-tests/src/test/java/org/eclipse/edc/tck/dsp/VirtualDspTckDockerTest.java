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

package org.eclipse.edc.tck.dsp;

import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import org.eclipse.edc.api.authentication.OauthServerEndToEndExtension;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.ManagementApiClientV5;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model.DataPlaneRegistrationDto;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.AuthorizationProfile;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.annotations.Runtime;
import org.eclipse.edc.junit.extensions.ComponentRuntimeContext;
import org.eclipse.edc.junit.extensions.ComponentRuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.utils.Endpoints;
import org.eclipse.edc.nats.testfixtures.NatsEndToEndExtension;
import org.eclipse.edc.signaling.auth.Oauth2Extension;
import org.eclipse.edc.signaling.client.DataPlaneSignalingTestClient;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndExtension;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.eclipse.edc.util.io.Ports.getFreePort;

@PostgresqlIntegrationTest
@Testcontainers
public class VirtualDspTckDockerTest extends DspTckDockerTest {

    public static final String PARTICIPANT_CONTEXT_ID = "participantContextId";

    @RegisterExtension
    @Order(0)
    static final Oauth2Extension OAUTH_SERVER = new Oauth2Extension();

    @Order(0)
    @RegisterExtension
    static final OauthServerEndToEndExtension AUTH_SERVER_EXTENSION = OauthServerEndToEndExtension.Builder.newInstance().build();

    @Order(0)
    @RegisterExtension
    static final PostgresqlEndToEndExtension POSTGRESQL_EXTENSION = new PostgresqlEndToEndExtension();

    @Order(0)
    @RegisterExtension
    static final NatsEndToEndExtension NATS_EXTENSION = new NatsEndToEndExtension();

    @RegisterExtension
    static final RuntimeExtension RUNTIME = ComponentRuntimeExtension.Builder.newInstance()
            .name(ControlPlane.NAME)
            .modules(ControlPlane.MODULES)
            .endpoints(ControlPlane.ENDPOINTS.build())
            .configurationProvider(VirtualDspTckDockerTest::virtualRuntimeConfiguration)
            .configurationProvider(() -> POSTGRESQL_EXTENSION.configFor(ControlPlane.NAME.toLowerCase()))
            .configurationProvider(NATS_EXTENSION::configFor)
            .configurationProvider(AUTH_SERVER_EXTENSION::getConfig)
            .paramProvider(ManagementApiClientV5.class, (ctx) -> ManagementApiClientV5.forContext(ctx, AUTH_SERVER_EXTENSION.getAuthServer()))
            .build();
    @RegisterExtension
    @Order(1)
    static final RuntimeExtension PROVIDER_DATA_PLANE = ComponentRuntimeExtension.Builder.newInstance()
            .name("dataplane")
            .modules(SignalingDataPlane.MODULES)
            .endpoints(SignalingDataPlane.ENDPOINTS.build())
            .configurationProvider(SignalingDataPlane::config)
            .paramProvider(DataPlaneSignalingTestClient.class, DataPlaneSignalingTestClient::new)
            .build();
    @Order(1)
    @RegisterExtension
    static final BeforeAllCallback SETUP = context -> {
        POSTGRESQL_EXTENSION.createDatabase(ControlPlane.NAME.toLowerCase());
    };

    private static Config virtualRuntimeConfiguration() {
        return ConfigFactory.fromMap(new HashMap<>() {
            {
                put("edc.participant.id", PARTICIPANT_CONTEXT_ID);
                put("web.http.protocol.port", "8282"); // this must match the configured connector url in resources/docker.tck.properties
                put("web.http.protocol.path", "/api/dsp"); // this must match the configured connector url in resources/docker.tck.properties
                put("edc.dsp.callback.address", "http://host.docker.internal:8282/api/dsp/%s"); // host.docker.internal is required by the container to communicate with the host
                put("edc.hostname", "host.docker.internal");
                put("edc.component.id", "DSP-compatibility-test");
                put("edc.transfer.proxy.token.signer.privatekey.alias", "private-key");
                put("edc.transfer.proxy.token.verifier.publickey.alias", "public-key");
                put("edc.iam.oauth2.jwks.url", "https://example.com/jwks");
                put("edc.iam.oauth2.issuer", "test-issuer");
            }
        });
    }

    @BeforeAll
    static void beforeAll(ManagementApiClientV5 connectorClient,
                          DataPlaneSignalingTestClient dataplaneClient,
                          Oauth2Extension oauth2,
                          @Runtime(ControlPlane.NAME) ComponentRuntimeContext ctx) {

        var signalingProtocol = ctx.getEndpoint("signaling");

        var dataplaneOauth2Profile = oauth2.registerClient(dataplaneClient.dataPlaneId());
        var controlPlaneOauth2Profile = oauth2.registerClient(PARTICIPANT_CONTEXT_ID);

        var dp = new DataPlaneRegistrationDto(
                dataplaneClient.dataPlaneId(),
                dataplaneClient.getDataFlowsEndpoint(),
                Set.of("NonFinite-PULL"),
                Set.of(),
                toAuthorizationProfile(controlPlaneOauth2Profile).properties()
        );
        connectorClient.dataplanes().registerDataPlane(PARTICIPANT_CONTEXT_ID, dp);

        dataplaneClient.registerControlPlane(createObjectBuilder()
                .add("controlplaneId", PARTICIPANT_CONTEXT_ID)
                .add("endpoint", signalingProtocol.get().toString())
                .add("authorization", createArrayBuilder().add(dataplaneOauth2Profile))
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

    protected @NonNull String getConfigFile() {
        return "docker.tck.virtual.properties";
    }


    interface ControlPlane {

        String NAME = "CUT";

        String[] MODULES = new String[]{
                ":dist:bom:controlplane-virtual-base-bom",
                ":dist:bom:controlplane-feature-sql-bom",
                ":dist:bom:controlplane-virtual-feature-sql-bom",
                ":dist:bom:controlplane-virtual-feature-nats-bom",
                ":extensions:common:iam:iam-mock",
                ":system-tests:tck:tasks-tck-extension"
        };

        Endpoints.Builder ENDPOINTS = Endpoints.Builder.newInstance()
                .endpoint("management", () -> URI.create("http://localhost:" + getFreePort() + "/management"))
                .endpoint("signaling", () -> URI.create("http://localhost:" + getFreePort() + "/signaling"));
    }

    interface SignalingDataPlane {
        String[] MODULES = new String[]{
                ":system-tests:e2e-transfer-test:signaling-data-plane"
        };

        Endpoints.Builder ENDPOINTS = Endpoints.Builder.newInstance()
                .endpoint("default", () -> URI.create("http://localhost:" + getFreePort() + "/api"));

        static Config config() {
            return ConfigFactory.fromMap(Map.of("dataplane.id", UUID.randomUUID().toString()));
        }
    }
}

