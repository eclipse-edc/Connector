/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.test.bom;

import io.nats.client.NKey;
import io.nats.client.Nats;
import io.nats.client.Options;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.ConsumerContractNegotiationManager;
import org.eclipse.edc.connector.controlplane.transfer.spi.TransferProcessManager;
import org.eclipse.edc.controlplane.StateMachineFeature;
import org.eclipse.edc.controlplane.tasks.TaskService;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimePerMethodExtension;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.security.SignatureService;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndExtension;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.vault.VaultContainer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.hamcrest.Matchers.equalTo;

public class BomSmokeTests {
    abstract static class SmokeTest {
        public static final String DEFAULT_PORT = "8080";
        public static final String DEFAULT_PATH = "/api";

        @Test
        void assertRuntimeReady() {
            await().untilAsserted(() -> given()
                    .baseUri("http://localhost:" + DEFAULT_PORT + DEFAULT_PATH + "/check/startup")
                    .get()
                    .then()
                    .statusCode(200)
                    .log().ifValidationFails()
                    .body("isSystemHealthy", equalTo(true)));

        }
    }

    @Nested
    @EndToEndTest
    class ControlPlaneDcp extends SmokeTest {

        @RegisterExtension
        protected RuntimeExtension runtime = new RuntimePerMethodExtension(
                new EmbeddedRuntime("control-plane-dcp-bom", ":dist:bom:controlplane-dcp-bom")
                        .configurationProvider(() -> ConfigFactory.fromMap(new HashMap<>() {{
                                    put("edc.iam.sts.oauth.token.url", "https://sts.com/token");
                                    put("edc.iam.sts.oauth.client.id", "test-client");
                                    put("edc.iam.sts.oauth.client.secret.alias", "test-alias");
                                    put("web.http.port", DEFAULT_PORT);
                                    put("web.http.path", DEFAULT_PATH);
                                    put("web.http.management.port", "8081");
                                    put("web.http.management.path", "/api/management");
                                    put("edc.iam.sts.privatekey.alias", "privatekey");
                                    put("edc.iam.sts.publickey.id", "publickey");
                                    put("edc.participant.did", "did:web:someone");
                                }})
                        )
        );
    }

    @Nested
    @EndToEndTest
    class FederatedCatalogDcp extends SmokeTest {

        @RegisterExtension
        protected RuntimeExtension runtime =
                new RuntimePerMethodExtension(new EmbeddedRuntime("fc-dcp-bom", ":dist:bom:federatedcatalog-dcp-bom")
                        .configurationProvider(() -> ConfigFactory.fromMap(new HashMap<>() {
                            {
                                put("edc.iam.sts.oauth.token.url", "https://sts.com/token");
                                put("edc.iam.sts.oauth.client.id", "test-clientid");
                                put("edc.iam.sts.oauth.client.secret.alias", "test-alias");
                                put("web.http.port", DEFAULT_PORT);
                                put("web.http.path", DEFAULT_PATH);
                                put("web.http.catalog.port", "8081");
                                put("web.http.catalog.path", "/api/catalog");
                                put("web.http.version.port", String.valueOf(getFreePort()));
                                put("edc.catalog.cache.execution.period.seconds", "5");
                                put("edc.participant.did", "did:web:testparticipant");
                                put("edc.iam.sts.privatekey.alias", "private-alias");
                                put("edc.iam.sts.publickey.id", "public-key-id");
                                put("edc.catalog.cache.execution.delay.seconds", "0");
                            }
                        }))
                );
    }

    @Nested
    @EndToEndTest
    class VirtualControlPlaneDcp extends SmokeTest {

        @RegisterExtension
        protected RuntimeExtension runtime = new RuntimePerMethodExtension(
                new EmbeddedRuntime("virtual-control-plane-dcp-bom",
                        ":dist:bom:controlplane-virtual-base-bom",
                        ":dist:bom:controlplane-virtual-feature-dcp-bom")
                        .configurationProvider(() -> ConfigFactory.fromMap(new HashMap<>() {{
                                    put("edc.iam.sts.oauth.token.url", "https://sts.com/token");
                                    put("edc.iam.sts.oauth.client.id", "test-client");
                                    put("edc.iam.sts.oauth.client.secret.alias", "test-alias");
                                    put("web.http.port", DEFAULT_PORT);
                                    put("web.http.path", DEFAULT_PATH);
                                    put("web.http.management.port", "8081");
                                    put("web.http.management.path", "/api/management");
                                    put("edc.iam.sts.privatekey.alias", "privatekey");
                                    put("edc.iam.sts.publickey.id", "publickey");
                                    put("edc.participant.did", "did:web:someone");
                                    put("edc.iam.oauth2.jwks.url", "https://example.com/jwks");
                                    put("edc.iam.oauth2.issuer", "test-issuer");
                                    put("web.http.protocol.virtual", "true");
                                    // this BOM only contains the task-based state machine, which is not loaded by default
                                    put(StateMachineFeature.FEATURE_KEY, StateMachineFeature.TASK);
                                }})
                        )
        );
    }

    @Nested
    @EndToEndTest
    class VirtualPollControlPlaneDcp extends SmokeTest {

        @RegisterExtension
        protected RuntimeExtension runtime = new RuntimePerMethodExtension(
                new EmbeddedRuntime("virtual-poll-control-plane-dcp-bom",
                        ":dist:bom:controlplane-virtual-poll-base-bom",
                        ":dist:bom:controlplane-virtual-feature-dcp-bom")
                        .configurationProvider(() -> ConfigFactory.fromMap(new HashMap<>() {{
                                    put("edc.iam.sts.oauth.token.url", "https://sts.com/token");
                                    put("edc.iam.sts.oauth.client.id", "test-client");
                                    put("edc.iam.sts.oauth.client.secret.alias", "test-alias");
                                    put("web.http.port", DEFAULT_PORT);
                                    put("web.http.path", DEFAULT_PATH);
                                    put("web.http.management.port", "8081");
                                    put("web.http.management.path", "/api/management");
                                    put("edc.iam.sts.privatekey.alias", "privatekey");
                                    put("edc.iam.sts.publickey.id", "publickey");
                                    put("edc.participant.did", "did:web:someone");
                                    put("edc.iam.oauth2.jwks.url", "https://example.com/jwks");
                                    put("edc.iam.oauth2.issuer", "test-issuer");
                                    put("web.http.protocol.virtual", "true");
                                }})
                        )
        );
    }

    /**
     * The all-in-one BOM with every feature enabled: SQL persistence, the HashiCorp Vault, the task-based state machine on
     * NATS, NATS event publishing and NKey authentication of the NATS connections.
     */
    @Nested
    @EndToEndTest
    class ControlPlaneAllInOne extends SmokeTest {

        private static final String EVENTS_STREAM = "events";
        private static final String VAULT_TOKEN = "root";

        @RegisterExtension
        static final PostgresqlEndToEndExtension POSTGRES = new PostgresqlEndToEndExtension();

        private static VaultContainer<?> vault;
        private static GenericContainer<?> nats;
        private static NKey nkey;
        private static Path nkeySeedFile;

        @RegisterExtension
        protected RuntimeExtension runtime = new RuntimePerMethodExtension(
                new EmbeddedRuntime("control-plane-all-in-one", ":dist:bom:controlplane-all-in-one")
                        .configurationProvider(POSTGRES::config)
                        .configurationProvider(() -> ConfigFactory.fromMap(new HashMap<>() {{
                                    put("edc.iam.sts.oauth.token.url", "https://sts.com/token");
                                    put("edc.iam.sts.oauth.client.id", "test-client");
                                    put("edc.iam.sts.oauth.client.secret.alias", "test-alias");
                                    put("web.http.port", DEFAULT_PORT);
                                    put("web.http.path", DEFAULT_PATH);
                                    put("web.http.management.port", "8081");
                                    put("web.http.management.path", "/api/management");
                                    put("edc.iam.sts.privatekey.alias", "privatekey");
                                    put("edc.iam.sts.publickey.id", "publickey");
                                    put("edc.participant.did", "did:web:someone");
                                    put("edc.iam.oauth2.jwks.url", "https://example.com/jwks");
                                    put("edc.iam.oauth2.issuer", "test-issuer");
                                    put("web.http.protocol.virtual", "true");

                                    // edc.feature.persistence=postgres is contributed by the Postgres extension in line 204

                                    put("edc.feature.vault", "hashicorp");
                                    put("edc.vault.hashicorp.url", "http://%s:%d".formatted(vault.getHost(), vault.getMappedPort(8200)));
                                    put("edc.vault.hashicorp.token", VAULT_TOKEN);

                                    put(StateMachineFeature.FEATURE_KEY, StateMachineFeature.TASK);
                                    put("edc.nats.cn.publisher.url", natsUrl());
                                    put("edc.nats.cn.subscriber.url", natsUrl());
                                    put("edc.nats.cn.subscriber.autocreate", "true");
                                    put("edc.nats.tp.publisher.url", natsUrl());
                                    put("edc.nats.tp.subscriber.url", natsUrl());
                                    put("edc.nats.tp.subscriber.autocreate", "true");

                                    put("edc.feature.events", "nats");
                                    put("edc.events.nats.url", natsUrl());
                                    put("edc.events.nats.stream", EVENTS_STREAM);

                                    put("edc.feature.nats.auth", "nkey");
                                    put("edc.nats.auth.nkey.seed.path", nkeySeedFile.toString());
                                }})
                        )
        );

        @BeforeAll
        static void startInfrastructure() throws Exception {
            vault = new VaultContainer<>("hashicorp/vault:1.18.3").withVaultToken(VAULT_TOKEN);
            vault.start();

            // the NATS server only accepts connections that authenticate with the NKey
            nkey = NKey.createUser(new SecureRandom());
            nkeySeedFile = Files.createTempFile("nats", ".nk");
            Files.writeString(nkeySeedFile, new String(nkey.getSeed()));
            var natsConf = """
                    port: 4222
                    jetstream {}
                    authorization {
                      users [
                        { nkey: "%s" }
                      ]
                    }
                    """.formatted(new String(nkey.getPublicKey()));
            nats = new GenericContainer<>("nats:2.9.22")
                    .withCopyToContainer(Transferable.of(natsConf), "/etc/nats/nats.conf")
                    .withCommand("-c", "/etc/nats/nats.conf")
                    .withExposedPorts(4222)
                    .waitingFor(Wait.forListeningPort());
            nats.start();
        }

        @AfterAll
        static void stopInfrastructure() throws Exception {
            nats.stop();
            vault.stop();
            Files.deleteIfExists(nkeySeedFile);
        }

        private static String natsUrl() {
            return "nats://%s:%d".formatted(nats.getHost(), nats.getMappedPort(4222));
        }

        @Test
        @Override
        void assertRuntimeReady() {
            super.assertRuntimeReady();

            // persistence
            assertThat(runtime.getService(QueryExecutor.class)).isNotNull();
            // vault: the HashiCorp Vault extension is the only provider of the signature service
            assertThat(runtime.getService(SignatureService.class)).isNotNull();
            // state machine: task-based instead of polling
            assertThat(runtime.getService(TaskService.class)).isNotNull();
            assertThatThrownBy(() -> runtime.getService(TransferProcessManager.class)).isInstanceOf(EdcException.class);
            // NATS authentication: the NKey extension contributes the connection options
            assertThat(runtime.getService(Options.class)).isNotNull();
            // event publishing and the task-based state machine created their streams
            assertThat(natsStreams()).contains(EVENTS_STREAM, "cn-stream", "tp-stream");
        }

        private List<String> natsStreams() {
            var options = new Options.Builder()
                    .server(natsUrl())
                    .authHandler(Nats.staticCredentials(null, nkey.getSeed()))
                    .build();
            try (var connection = Nats.connect(options)) {
                return connection.jetStreamManagement().getStreamNames();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * The all-in-one BOM without any {@code edc.feature.*} key: it must start without additional infrastructure, using
     * the polling-based state machine, the in-memory stores and the in-memory vault.
     */
    @Nested
    @EndToEndTest
    class ControlPlaneAllInOneDefaults extends SmokeTest {

        @RegisterExtension
        protected RuntimeExtension runtime = new RuntimePerMethodExtension(
                new EmbeddedRuntime("control-plane-all-in-one-defaults", ":dist:bom:controlplane-all-in-one")
                        .configurationProvider(() -> ConfigFactory.fromMap(new HashMap<>() {{
                                    put("edc.iam.sts.oauth.token.url", "https://sts.com/token");
                                    put("edc.iam.sts.oauth.client.id", "test-client");
                                    put("edc.iam.sts.oauth.client.secret.alias", "test-alias");
                                    put("web.http.port", DEFAULT_PORT);
                                    put("web.http.path", DEFAULT_PATH);
                                    put("web.http.management.port", "8081");
                                    put("web.http.management.path", "/api/management");
                                    put("edc.iam.sts.privatekey.alias", "privatekey");
                                    put("edc.iam.sts.publickey.id", "publickey");
                                    put("edc.participant.did", "did:web:someone");
                                    put("edc.iam.oauth2.jwks.url", "https://example.com/jwks");
                                    put("edc.iam.oauth2.issuer", "test-issuer");
                                    put("web.http.protocol.virtual", "true");
                                }})
                        )
        );

        @Test
        @Override
        void assertRuntimeReady() {
            super.assertRuntimeReady();

            // state machine: polling instead of task-based
            assertThat(runtime.getService(TransferProcessManager.class)).isNotNull();
            assertThat(runtime.getService(ConsumerContractNegotiationManager.class)).isNotNull();
            assertThatThrownBy(() -> runtime.getService(TaskService.class)).isInstanceOf(EdcException.class);
            // neither SQL persistence, nor the HashiCorp Vault, nor the NKey authentication
            assertThatThrownBy(() -> runtime.getService(QueryExecutor.class)).isInstanceOf(EdcException.class);
            assertThatThrownBy(() -> runtime.getService(SignatureService.class)).isInstanceOf(EdcException.class);
            assertThatThrownBy(() -> runtime.getService(Options.class)).isInstanceOf(EdcException.class);
        }
    }

}
