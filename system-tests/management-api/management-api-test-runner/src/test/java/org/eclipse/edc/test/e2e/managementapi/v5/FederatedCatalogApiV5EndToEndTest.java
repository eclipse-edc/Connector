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

package org.eclipse.edc.test.e2e.managementapi.v5;

import io.restassured.http.ContentType;
import org.eclipse.edc.api.authentication.OauthServer;
import org.eclipse.edc.api.authentication.OauthServerEndToEndExtension;
import org.eclipse.edc.catalog.spi.CatalogConstants;
import org.eclipse.edc.catalog.spi.FederatedCatalogCache;
import org.eclipse.edc.connector.controlplane.catalog.spi.Catalog;
import org.eclipse.edc.connector.controlplane.catalog.spi.DataService;
import org.eclipse.edc.connector.controlplane.catalog.spi.Dataset;
import org.eclipse.edc.connector.controlplane.catalog.spi.Distribution;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.ComponentRuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextService;
import org.eclipse.edc.test.e2e.managementapi.Runtimes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.UUID;

import static org.eclipse.edc.test.e2e.managementapi.v5.TestFunction.createParticipant;
import static org.hamcrest.Matchers.is;

/**
 * Federated catalog cache v5 endpoints end-to-end tests. Exercises the participant-scoped
 * {@code /v5/participants/{participantContextId}/catalogs/request} endpoint in a multi-participant runtime, verifying
 * that each participant only sees the catalogs crawled on its behalf and that the authorization rules apply.
 */
public class FederatedCatalogApiV5EndToEndTest {

    @SuppressWarnings("JUnitMalformedDeclaration")
    abstract static class Tests {

        private static final String PARTICIPANT_CONTEXT_ID = "test-participant";

        private String participantToken;

        @BeforeEach
        void setup(OauthServer authServer, ParticipantContextService participantContextService, FederatedCatalogCache cache) {
            createParticipant(participantContextService, PARTICIPANT_CONTEXT_ID);
            participantToken = authServer.createToken(PARTICIPANT_CONTEXT_ID);
            // seed the cache as the crawler would: two catalogs for our participant, one for a foreign participant
            cache.save(PARTICIPANT_CONTEXT_ID, catalog("catalog-a1", "http://provider-a1"));
            cache.save(PARTICIPANT_CONTEXT_ID, catalog("catalog-a2", "http://provider-a2"));
        }

        @AfterEach
        void teardown(ParticipantContextService participantContextService) {
            participantContextService.deleteParticipantContext(PARTICIPANT_CONTEXT_ID);
        }

        @Test
        void requestCatalogs_shouldReturnOnlyOwnParticipantsCatalogs(ManagementEndToEndV5TestContext context,
                                                                     OauthServer authServer,
                                                                     ParticipantContextService srv,
                                                                     FederatedCatalogCache cache) {
            var otherParticipant = UUID.randomUUID().toString();
            createParticipant(srv, otherParticipant);
            var otherToken = authServer.createToken(otherParticipant);
            cache.save(otherParticipant, catalog("catalog-b1", "http://provider-b1"));

            context.baseRequest(participantToken)
                    .contentType(ContentType.JSON)
                    .body(context.query().toString())
                    .post("/v5/participants/" + PARTICIPANT_CONTEXT_ID + "/catalogs/request")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("size()", is(2));

            context.baseRequest(otherToken)
                    .contentType(ContentType.JSON)
                    .body(context.query().toString())
                    .post("/v5/participants/" + otherParticipant + "/catalogs/request")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("size()", is(1));
        }

        @Test
        void requestCatalogs_whenTokenBearerDoesNotOwnParticipant_shouldReturn403(ManagementEndToEndV5TestContext context,
                                                                                  OauthServer authServer,
                                                                                  ParticipantContextService srv) {
            var otherParticipant = UUID.randomUUID().toString();
            createParticipant(srv, otherParticipant);
            var otherToken = authServer.createToken(otherParticipant);

            context.baseRequest(otherToken)
                    .contentType(ContentType.JSON)
                    .body(context.query().toString())
                    .post("/v5/participants/" + PARTICIPANT_CONTEXT_ID + "/catalogs/request")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(403);
        }

        @Test
        void requestCatalogs_whenTokenBearerIsAdmin_shouldSucceed(ManagementEndToEndV5TestContext context, OauthServer authServer) {
            var adminToken = authServer.createAdminToken();

            context.baseRequest(adminToken)
                    .contentType(ContentType.JSON)
                    .body(context.query().toString())
                    .post("/v5/participants/" + PARTICIPANT_CONTEXT_ID + "/catalogs/request")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("size()", is(2));
        }

        private Catalog catalog(String id, String originator) {
            var dataService = DataService.Builder.newInstance().endpointUrl("http://dataservice").build();
            var dataset = Dataset.Builder.newInstance()
                    .id(id + "-dataset")
                    .distribution(Distribution.Builder.newInstance().dataService(dataService).format("test-format").build())
                    .build();
            return Catalog.Builder.newInstance()
                    .id(id)
                    .participantId("provider")
                    .dataServices(List.of(dataService))
                    .datasets(List.of(dataset))
                    .property(CatalogConstants.PROPERTY_ORIGINATOR, originator)
                    .build();
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
                .modules(":core:catalog-crawler:catalog-crawler-core",
                        ":extensions:federated-catalog:api:federated-catalog-api")
                .endpoints(Runtimes.ControlPlane.ENDPOINTS.build())
                .configurationProvider(Runtimes.ControlPlane::config)
                .configurationProvider(AUTH_SERVER_EXTENSION::getConfig)
                .paramProvider(ManagementEndToEndV5TestContext.class, ManagementEndToEndV5TestContext::forContext)
                .build();
    }

}
