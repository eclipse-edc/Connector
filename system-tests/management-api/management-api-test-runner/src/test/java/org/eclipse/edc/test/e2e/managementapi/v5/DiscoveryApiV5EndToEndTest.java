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

package org.eclipse.edc.test.e2e.managementapi.v5;

import jakarta.json.JsonArray;
import org.eclipse.edc.api.authentication.OauthServer;
import org.eclipse.edc.api.authentication.OauthServerEndToEndExtension;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.ComponentRuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.participantcontext.spi.config.model.ParticipantContextConfiguration;
import org.eclipse.edc.participantcontext.spi.config.store.ParticipantContextConfigStore;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextService;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContextState;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.test.e2e.managementapi.Runtimes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.http.ContentType.JSON;
import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2;

public class DiscoveryApiV5EndToEndTest {

    @SuppressWarnings("JUnitMalformedDeclaration")
    abstract static class Tests {


        private static final List<String> PROFILES = List.of("dsp2025_1", "dsp2025_2");

        @BeforeAll
        static void setup() {

        }

        @AfterEach
        void teardown(ParticipantContextService participantContextService) {
            var list = participantContextService.search(QuerySpec.max())
                    .orElseThrow(f -> new AssertionError(f.getFailureDetail()));

            for (var p : list) {
                participantContextService.deleteParticipantContext(p.getParticipantContextId()).orElseThrow(f -> new AssertionError(f.getFailureDetail()));
            }
        }

        @Test
        void discover(ManagementEndToEndV5TestContext context, ParticipantContextService participantContextService,
                      ParticipantContextConfigStore configStore,
                      OauthServer authServer) {

            var providerContextId = "provider-context-" + UUID.randomUUID();
            var consumerContextId = "consumer-context-" + UUID.randomUUID();

            var participantTokenJwt = authServer.createToken(consumerContextId);

            createParticipant(participantContextService, configStore, consumerContextId, List.of("dsp2025_1"));
            createParticipant(participantContextService, configStore, providerContextId, PROFILES);

            var discovery = fetchDiscovery(context, providerContextId, participantTokenJwt, consumerContextId);


            assertThat(discovery).hasSize(1).allSatisfy(entry -> {
                assertThat(entry.asJsonObject().getString(TYPE)).isEqualTo("DiscoveryResponse");
                assertThat(entry.asJsonObject().getString("profile")).isEqualTo("dsp2025_1");
                assertThat(entry.asJsonObject().getString("version")).isEqualTo("2025-1");
                assertThat(entry.asJsonObject().getString("binding")).isNotNull();
                var counterParty = entry.asJsonObject().getJsonObject("counterParty");
                assertThat(counterParty).isNotNull();
                assertThat(counterParty.getString("path")).isNotNull();
                assertThat(counterParty.getString("dataServiceEndpoint")).isNotNull();
            });
        }

        @Test
        void discover_validationFails(ManagementEndToEndV5TestContext context, ParticipantContextService participantContextService,
                                      ParticipantContextConfigStore configStore,
                                      OauthServer authServer) {

            var providerContextId = "provider-context-" + UUID.randomUUID();
            var consumerContextId = "consumer-context-" + UUID.randomUUID();

            var participantTokenJwt = authServer.createToken(consumerContextId);

            createParticipant(participantContextService, configStore, consumerContextId, List.of("dsp2025_1"));
            createParticipant(participantContextService, configStore, providerContextId, PROFILES);

            var requestBody = createObjectBuilder()
                    .add(CONTEXT, createArrayBuilder().add(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2))
                    .add(TYPE, "DiscoveryRequest")
                    .build()
                    .toString();

            context.baseRequest(participantTokenJwt)
                    .contentType(JSON)
                    .body(requestBody)
                    .post("/v5beta/participants/%s/discover/request".formatted(consumerContextId))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(400)
                    .contentType(JSON);
        }


        private void createParticipant(ParticipantContextService participantContextService,
                                       ParticipantContextConfigStore configStore, String participantContextId, List<String> profiles) {
            var pc = ParticipantContext.Builder.newInstance()
                    .participantContextId(participantContextId)
                    .state(ParticipantContextState.ACTIVATED)
                    .identity(participantContextId)
                    .build();

            var config = ParticipantContextConfiguration.Builder.newInstance()
                    .participantContextId(participantContextId)
                    .entries(Map.of("edc.mock.region", "eu",
                            "edc.participant.id", "did:web:" + participantContextId,
                            "edc.dataspace.profiles", String.join(",", profiles)
                    ))
                    .build();

            configStore.save(config);

            participantContextService.createParticipantContext(pc)
                    .orElseThrow(f -> new AssertionError(f.getFailureDetail()));
        }

        private JsonArray fetchDiscovery(ManagementEndToEndV5TestContext context, String providerContextId, String participantTokenJwt, String consumerContextId) {
            var requestBody = createObjectBuilder()
                    .add(CONTEXT, createArrayBuilder().add(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2))
                    .add(TYPE, "DiscoveryRequest")
                    .add("counterPartyAddress", context.providerBaseProtocolUrl(providerContextId))
                    .build()
                    .toString();

            return context.baseRequest(participantTokenJwt)
                    .contentType(JSON)
                    .body(requestBody)
                    .post("/v5beta/participants/%s/discover/request".formatted(consumerContextId))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .contentType(JSON)
                    .extract().body().as(JsonArray.class);
        }

    }

    @Nested
    @EndToEndTest
    class InMemory extends Tests {

        @Order(0)
        @RegisterExtension
        static final OauthServerEndToEndExtension AUTH_SERVER_EXTENSION = OauthServerEndToEndExtension.Builder.newInstance().build();

        @Order(1)
        @RegisterExtension
        static RuntimeExtension runtime = ComponentRuntimeExtension.Builder.newInstance()
                .name(Runtimes.ControlPlane.NAME)
                .modules(Runtimes.ControlPlane.VIRTUAL_MODULES)
                .endpoints(Runtimes.ControlPlane.ENDPOINTS.build())
                .configurationProvider(InMemory::config)
                .configurationProvider(AUTH_SERVER_EXTENSION::getConfig)
                .paramProvider(ManagementEndToEndV5TestContext.class,
                        ManagementEndToEndV5TestContext::forContext)
                .build();

        static Config config() {
            return ConfigFactory.fromMap(Map.of(
                            "edc.dataspace.profiles.dsp2025_1.name", "dsp2025_1",
                            "edc.dataspace.profiles.dsp2025_1.protocol.version", "2025-1",
                            "edc.dataspace.profiles.dsp2025_1.protocol.binding", "HTTPS",
                            "edc.dataspace.profiles.dsp2025_1.protocol.namespace", "https://w3id.org/dspace/2025/1/",
                            "edc.dataspace.profiles.dsp2025_1.jsonld.context.urls", "https://w3id.org/dspace/2025/1/context.jsonld",
                            "edc.dataspace.profiles.dsp2025_2.name", "dsp2025_2",
                            "edc.dataspace.profiles.dsp2025_2.protocol.version", "2025-1",
                            "edc.dataspace.profiles.dsp2025_2.protocol.binding", "HTTPS",
                            "edc.dataspace.profiles.dsp2025_2.protocol.namespace", "https://w3id.org/dspace/2025/1/",
                            "edc.dataspace.profiles.dsp2025_2.jsonld.context.urls", "https://w3id.org/dspace/2025/1/context.jsonld"
                    )
            );
        }
    }

}