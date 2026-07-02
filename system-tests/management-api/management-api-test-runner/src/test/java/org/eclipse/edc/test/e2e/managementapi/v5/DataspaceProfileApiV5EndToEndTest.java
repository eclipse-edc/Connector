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

import io.restassured.http.ContentType;
import jakarta.json.JsonObject;
import org.eclipse.edc.api.authentication.OauthServer;
import org.eclipse.edc.api.authentication.OauthServerEndToEndExtension;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.extensions.ComponentRuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.protocol.spi.service.DataspaceProfileService;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndExtension;
import org.eclipse.edc.test.e2e.managementapi.Runtimes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

public class DataspaceProfileApiV5EndToEndTest {

    @SuppressWarnings("JUnitMalformedDeclaration")
    abstract static class Tests {

        @AfterEach
        void tearDown(DataspaceProfileService service) {
            service.search(QuerySpec.max()).getContent()
                    .forEach(profile -> service.deleteById(profile.getName()));
        }

        @Test
        void createGetQueryDelete(ManagementEndToEndV5TestContext context, OauthServer authServer) {
            var token = authServer.createAdminToken();
            var name = "custom-profile";

            context.baseRequest(token)
                    .contentType(ContentType.JSON)
                    .body(profileJson(name))
                    .post("/v5beta/dataspaceprofiles")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .body("@type", equalTo("DataspaceProfile"))
                    .body("name", equalTo(name));

            context.baseRequest(token)
                    .contentType(ContentType.JSON)
                    .get("/v5beta/dataspaceprofiles/" + name)
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .body("name", equalTo(name))
                    .body("protocol.version", equalTo("2025-1"))
                    .body("protocol.binding", equalTo("HTTPS"))
                    .body("protocol.path", equalTo("/" + name))
                    .body("protocol.namespace", equalTo("https://w3id.org/dspace/2025/1/"))
                    .body("jsonLdContextsUrl", equalTo(List.of("https://w3id.org/dspace/2025/1/context.jsonld")));

            context.baseRequest(token)
                    .contentType(ContentType.JSON)
                    .body(context.query())
                    .post("/v5beta/dataspaceprofiles/request")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .body("size()", equalTo(1))
                    .body("name", hasItem(name));

            context.baseRequest(token)
                    .delete("/v5beta/dataspaceprofiles/" + name)
                    .then()
                    .log().ifValidationFails()
                    .statusCode(204);

            context.baseRequest(token)
                    .get("/v5beta/dataspaceprofiles/" + name)
                    .then()
                    .statusCode(404);
        }

        @Test
        void create_shouldReturnConflict_whenProfileAlreadyExists(ManagementEndToEndV5TestContext context, OauthServer authServer) {
            var token = authServer.createAdminToken();
            var name = "duplicate-profile";

            context.baseRequest(token)
                    .contentType(ContentType.JSON)
                    .body(profileJson(name))
                    .post("/v5beta/dataspaceprofiles")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200);

            context.baseRequest(token)
                    .contentType(ContentType.JSON)
                    .body(profileJson(name))
                    .post("/v5beta/dataspaceprofiles")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(409);
        }

        @Test
        void delete_shouldReturnNotFound_whenMissing(ManagementEndToEndV5TestContext context, OauthServer authServer) {
            var token = authServer.createAdminToken();

            context.baseRequest(token)
                    .delete("/v5beta/dataspaceprofiles/unknown")
                    .then()
                    .statusCode(404);
        }

        private JsonObject profileJson(String name) {
            return createObjectBuilder()
                    .add(CONTEXT, createArrayBuilder().add(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2))
                    .add(TYPE, "DataspaceProfile")
                    .add("name", name)
                    .add("protocol", createObjectBuilder()
                            .add("version", "2025-1")
                            .add("path", "/" + name)
                            .add("binding", "HTTPS")
                            .add("namespace", "https://w3id.org/dspace/2025/1/"))
                    .add("jsonLdContextsUrl", createArrayBuilder().add("https://w3id.org/dspace/2025/1/context.jsonld"))
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
                .endpoints(Runtimes.ControlPlane.ENDPOINTS.build())
                .configurationProvider(Runtimes.ControlPlane::config)
                .paramProvider(ManagementEndToEndV5TestContext.class, ManagementEndToEndV5TestContext::forContext)
                .configurationProvider(AUTH_SERVER_EXTENSION::getConfig)
                .build();
    }

    @Nested
    @PostgresqlIntegrationTest
    class Postgres extends Tests {

        @Order(0)
        @RegisterExtension
        static final OauthServerEndToEndExtension AUTH_SERVER_EXTENSION = OauthServerEndToEndExtension.Builder.newInstance().build();

        @RegisterExtension
        @Order(0)
        static final PostgresqlEndToEndExtension POSTGRES_EXTENSION = new PostgresqlEndToEndExtension();

        @Order(1)
        @RegisterExtension
        static final BeforeAllCallback SETUP = context -> {
            POSTGRES_EXTENSION.createDatabase(Runtimes.ControlPlane.NAME.toLowerCase());
        };

        @Order(2)
        @RegisterExtension
        static RuntimeExtension runtime = ComponentRuntimeExtension.Builder.newInstance()
                .name(Runtimes.ControlPlane.NAME)
                .modules(Runtimes.ControlPlane.VIRTUAL_MODULES)
                .modules(Runtimes.ControlPlane.VIRTUAL_SQL_MODULES)
                .endpoints(Runtimes.ControlPlane.ENDPOINTS.build())
                .configurationProvider(Runtimes.ControlPlane::config)
                .configurationProvider(() -> POSTGRES_EXTENSION.configFor(Runtimes.ControlPlane.NAME.toLowerCase()))
                .configurationProvider(AUTH_SERVER_EXTENSION::getConfig)
                .paramProvider(ManagementEndToEndV5TestContext.class, ManagementEndToEndV5TestContext::forContext)
                .build();
    }
}
