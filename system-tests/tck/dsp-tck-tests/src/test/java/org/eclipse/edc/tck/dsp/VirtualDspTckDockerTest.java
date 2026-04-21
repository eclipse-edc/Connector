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

import org.eclipse.edc.api.authentication.OauthServerEndToEndExtension;
import org.eclipse.edc.connector.controlplane.test.system.utils.client.ManagementApiClientV5;
import org.eclipse.edc.junit.extensions.ComponentRuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.utils.Endpoints;
import org.eclipse.edc.nats.testfixtures.NatsEndToEndExtension;
import org.eclipse.edc.signaling.auth.Oauth2Extension;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndExtension;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.util.HashMap;

import static org.eclipse.edc.util.io.Ports.getFreePort;

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
            .modules(":system-tests:tck:dsp-tck-virtual-connector-under-test")
            .endpoints(ControlPlane.ENDPOINTS.build())
            .configurationProvider(VirtualDspTckDockerTest::virtualRuntimeConfiguration)
            .configurationProvider(() -> POSTGRESQL_EXTENSION.configFor(ControlPlane.NAME.toLowerCase()))
            .configurationProvider(NATS_EXTENSION::configFor)
            .configurationProvider(AUTH_SERVER_EXTENSION::getConfig)
            .paramProvider(ManagementApiClientV5.class, (ctx) -> ManagementApiClientV5.forContext(ctx, AUTH_SERVER_EXTENSION.getAuthServer()))
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
                put("edc.dsp.profiles.enable.all", "true");
            }
        });
    }

    protected @NonNull String getConfigFile() {
        return "docker.tck.virtual.properties";
    }


    interface ControlPlane {

        String NAME = "CUT";

        Endpoints.Builder ENDPOINTS = Endpoints.Builder.newInstance()
                .endpoint("management", () -> URI.create("http://localhost:" + getFreePort() + "/management"))
                .endpoint("signaling", () -> URI.create("http://localhost:" + getFreePort() + "/signaling"));
    }

}

