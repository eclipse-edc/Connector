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

import jakarta.json.JsonObject;
import org.eclipse.edc.api.authentication.OauthServer;
import org.eclipse.edc.api.authentication.OauthServerEndToEndExtension;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.ComponentRuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextService;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.test.e2e.managementapi.Runtimes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;

import static io.restassured.http.ContentType.JSON;
import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.test.e2e.managementapi.v5.TestFunction.createParticipant;
import static org.eclipse.edc.test.e2e.managementapi.v5.TestFunction.jsonLdContext;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

/**
 * End-to-end tests for the policy definition API with a custom JSON schema validator configured.
 * We load a custom policy definition schema where the `profile` field of a policy is required
 */
public class PolicyDefinitionApiCustomSchemaV5EndToEndTest {


    @SuppressWarnings("JUnitMalformedDeclaration")
    abstract static class Tests {

        private static final String PARTICIPANT_CONTEXT_ID = "test-participant";

        private String participantTokenJwt;

        @BeforeEach
        void setup(OauthServer authServer, ParticipantContextService participantContextService) {
            createParticipant(participantContextService, PARTICIPANT_CONTEXT_ID);

            participantTokenJwt = authServer.createToken(PARTICIPANT_CONTEXT_ID);
        }

        @AfterEach
        void teardown(ParticipantContextService participantContextService, PolicyDefinitionStore store) {
            participantContextService.deleteParticipantContext(PARTICIPANT_CONTEXT_ID)
                    .orElseThrow(f -> new AssertionError(f.getFailureDetail()));

            store.findAll(QuerySpec.max()).forEach(pd -> store.delete(pd.getId()));
        }

        @Test
        void create_validationFails(ManagementEndToEndV5TestContext context) {
            var requestBody = createObjectBuilder()
                    .add(CONTEXT, jsonLdContext())
                    .add(TYPE, "PolicyDefinition")
                    .add("policy", sampleOdrlPolicy())
                    .build();

            context.baseRequest(participantTokenJwt)
                    .body(requestBody.toString())
                    .contentType(JSON)
                    .post("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/policydefinitions")
                    .then()
                    .log().ifValidationFails()
                    .contentType(JSON)
                    .statusCode(400)
                    .body("size()", is(1))
                    .body("[0].message", containsString("required property 'assigner' not found"));
        }

        @Test
        void create_validationFailsWithCustomProfile(ManagementEndToEndV5TestContext context) {
            var requestBody = createObjectBuilder()
                    .add(CONTEXT, jsonLdContext())
                    .add(TYPE, "PolicyDefinition")
                    .add("policy", sampleOdrlPolicy("custom-profile"))
                    .build();

            context.baseRequest(participantTokenJwt)
                    .body(requestBody.toString())
                    .contentType(JSON)
                    .post("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/policydefinitions")
                    .then()
                    .log().ifValidationFails()
                    .contentType(JSON)
                    .statusCode(400)
                    .body("size()", is(2))
                    .body("[0].message", containsString("required property 'target' not found"))
                    .body("[1].message", containsString("required property 'assigner' not found"));
        }

        @Test
        void update_whenSchemaValidationFails(ManagementEndToEndV5TestContext context) {
            var requestBody = createObjectBuilder()
                    .add(CONTEXT, jsonLdContext())
                    .add(TYPE, "PolicyDefinition")
                    .add("policy", sampleOdrlPolicy())
                    .build();

            context.baseRequest(participantTokenJwt)
                    .body(requestBody.toString())
                    .contentType(JSON)
                    .put("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/policydefinitions/id")
                    .then()
                    .log().ifValidationFails()
                    .contentType(JSON)
                    .statusCode(400)
                    .body("size()", is(1))
                    .body("[0].message", containsString("required property 'assigner' not found"));
        }

        private JsonObject sampleOdrlPolicy() {
            return sampleOdrlPolicy(null);
        }

        private JsonObject sampleOdrlPolicy(String profile) {
            var builder = createObjectBuilder()
                    .add(TYPE, "Set")
                    .add("permission", createArrayBuilder()
                            .add(createObjectBuilder()
                                    .add("action", "use")
                                    .add("constraint", createArrayBuilder().add(createObjectBuilder()
                                                    .add("leftOperand", "inForceDate")
                                                    .add("operator", "gteq")
                                                    .add("rightOperand", "contractAgreement+0s"))
                                            .build()))
                            .build())
                    .add("prohibition", createArrayBuilder()
                            .add(createObjectBuilder()
                                    .add("action", "use")
                            ))
                    .add("obligation", createArrayBuilder()
                            .add(createObjectBuilder()
                                    .add("action", "use")
                            )
                    );

            if (profile != null) {
                builder.add("profile", profile);
            }

            return builder.build();

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
                .configurationProvider(InMemory::schemaValidatorConfig)
                .paramProvider(ManagementEndToEndV5TestContext.class, ManagementEndToEndV5TestContext::forContext)
                .configurationProvider(AUTH_SERVER_EXTENSION::getConfig)
                .build();

        static Config schemaValidatorConfig() {
            var uri = TestUtils.getFileFromResourceName("schema/v4").toURI().toString();
            uri = uri.endsWith("/") ? uri.substring(0, uri.length() - 1) : uri;
            return ConfigFactory.fromMap(Map.of(
                    "edc.mgmt.api.schema.custom.version", "v4",
                    "edc.mgmt.api.schema.custom.mapping.from", "https://example.org/schema/v4",
                    "edc.mgmt.api.schema.custom.mapping.to", uri,
                    "edc.mgmt.api.schema.custom.validator.policy.type", "PolicyDefinition",
                    "edc.mgmt.api.schema.custom.validator.policy.schema", "https://example.org/schema/v4/policy-definition-custom-schema.json",
                    "edc.mgmt.api.schema.custom.validator.policy1.type", "PolicyDefinition",
                    "edc.mgmt.api.schema.custom.validator.policy1.schema", "https://example.org/schema/v4/policy-definition-custom-profile-schema.json",
                    "edc.mgmt.api.schema.custom.validator.policy1.profiles", "custom-profile"
            ));
        }

    }

}
