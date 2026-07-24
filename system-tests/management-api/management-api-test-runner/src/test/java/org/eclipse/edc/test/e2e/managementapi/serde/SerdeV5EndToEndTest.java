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

package org.eclipse.edc.test.e2e.managementapi.serde;

import jakarta.json.Json;
import org.eclipse.edc.connector.controlplane.transform.edc.cel.from.JsonObjectFromCelExpressionTestResponseTransformer;
import org.eclipse.edc.connector.controlplane.transform.edc.cel.from.JsonObjectFromCelExpressionTransformer;
import org.eclipse.edc.connector.controlplane.transform.edc.cel.to.JsonObjectToCelExpressionTestRequestTransformer;
import org.eclipse.edc.connector.controlplane.transform.edc.cel.to.JsonObjectToCelExpressionTransformer;
import org.eclipse.edc.connector.controlplane.transform.edc.dataspaceprofile.from.JsonObjectFromDataspaceProfileContextTransformer;
import org.eclipse.edc.connector.controlplane.transform.edc.dataspaceprofile.from.JsonObjectToAssociateDataspaceProfileContextTransformer;
import org.eclipse.edc.connector.controlplane.transform.edc.participantcontext.config.from.JsonObjectFromParticipantContextConfigurationTransformer;
import org.eclipse.edc.connector.controlplane.transform.edc.participantcontext.config.to.JsonObjectToParticipantContextConfigurationTransformer;
import org.eclipse.edc.connector.controlplane.transform.edc.participantcontext.from.JsonObjectFromParticipantContextTransformer;
import org.eclipse.edc.connector.controlplane.transform.edc.participantcontext.to.JsonObjectToParticipantContextTransformer;
import org.eclipse.edc.iam.decentralizedclaims.spi.scope.DcpScope;
import org.eclipse.edc.iam.decentralizedclaims.spi.scope.DcpScopeRegistry;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.ComponentRuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.policy.cel.model.CelExpressionTestRequest;
import org.eclipse.edc.protocol.spi.AssociateDataspaceProfileContext;
import org.eclipse.edc.protocol.spi.DataspaceProfileContext;
import org.eclipse.edc.protocol.spi.ProtocolVersion;
import org.eclipse.edc.test.e2e.managementapi.Runtimes;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Map;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static jakarta.json.Json.createValue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.MANAGEMENT_API_CONTEXT;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.MANAGEMENT_API_SCOPE;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.associateDataspaceProfileObject;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.celExpressionTestRequest;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.createCelExpressionTestResponse;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.dcpScopeObject;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.dcpScopePolicyObject;
import static org.mockito.Mockito.mock;

@EndToEndTest
class SerdeV5EndToEndTest extends SerdeTestBase {

    @RegisterExtension
    static RuntimeExtension runtime = ComponentRuntimeExtension.Builder.newInstance()
            .name(Runtimes.ControlPlane.NAME)
            .modules(Runtimes.ControlPlane.MODULES)
            .modules(":extensions:common:api:management-api-schema-validator",
                    ":extensions:control-plane:api:management-api-v5:dcp-scope-api-v5")
            .endpoints(Runtimes.ControlPlane.ENDPOINTS.build())
            .build()
            .registerServiceMock(DcpScopeRegistry.class, mock());

    @BeforeAll
    static void beforeAll(TypeTransformerRegistry registry) {
        var factory = Json.createBuilderFactory(Map.of());
        registry.register(new JsonObjectFromParticipantContextTransformer(factory));
        registry.register(new JsonObjectToParticipantContextTransformer());
        registry.register(new JsonObjectFromParticipantContextConfigurationTransformer(factory));
        registry.register(new JsonObjectToParticipantContextConfigurationTransformer());
        registry.register(new JsonObjectFromCelExpressionTransformer(factory));
        registry.register(new JsonObjectFromCelExpressionTestResponseTransformer(factory));
        registry.register(new JsonObjectToCelExpressionTransformer());
        registry.register(new JsonObjectToCelExpressionTestRequestTransformer());
        registry.register(new JsonObjectFromDataspaceProfileContextTransformer(factory));
        registry.register(new JsonObjectToAssociateDataspaceProfileContextTransformer());
    }

    @Override
    protected List<String> transformerScope() {
        return List.of(MANAGEMENT_API_CONTEXT, "v4");
    }

    @Override
    protected String jsonLdScope() {
        return MANAGEMENT_API_SCOPE + ":v4";
    }

    @Override
    protected String schemaVersion() {
        return "v5";
    }

    @Override
    protected String jsonLdContext() {
        return EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2;
    }

    @Override
    protected boolean strictSchema() {
        return true;
    }


    @Test
    void ser_CelExpressionTestResponse(TypeTransformerRegistry typeTransformerRegistry, JsonObjectValidatorRegistry validatorRegistry, JsonLd jsonLd) {
        var response = createCelExpressionTestResponse();
        var compactResult = serialize(typeTransformerRegistry, validatorRegistry, jsonLd, response);

        assertThat(compactResult).isNotNull();
        assertThat(compactResult.getString(TYPE)).isEqualTo("CelExpressionTestResponse");
        assertThat(compactResult.getBoolean("evaluationResult")).isEqualTo(response.getEvaluationResult());
        assertThat(compactResult.getString("error")).isEqualTo(response.getError());

    }

    @Test
    void de_CelExpressionTestRequest(TypeTransformerRegistry typeTransformerRegistry, JsonObjectValidatorRegistry validatorRegistry, JsonLd jsonLd) {
        var inputObject = celExpressionTestRequest(jsonLdContext());
        var request = deserialize(typeTransformerRegistry, validatorRegistry, jsonLd, inputObject, CelExpressionTestRequest.class);

        assertThat(request).isNotNull();
        assertThat(request.getExpression()).isEqualTo(inputObject.getString("expression"));
        assertThat(request.getLeftOperand()).isEqualTo(inputObject.getString("leftOperand"));
        assertThat(request.getRightOperand()).isEqualTo(inputObject.getString("rightOperand"));
        assertThat(request.getOperator()).isEqualTo(inputObject.getString("operator"));
        assertThat(createObjectBuilder(request.getParams()).build()).containsAllEntriesOf(inputObject.getJsonObject("params"));

    }

    @Test
    void ser_DataspaceProfileContext(TypeTransformerRegistry typeTransformerRegistry, JsonObjectValidatorRegistry validatorRegistry, JsonLd jsonLd) {

        var protocol = new ProtocolVersion("test-protocol", "1.0", "https");

        var dataspaceProfile = new DataspaceProfileContext("test-profile", protocol, mock(), mock(), new JsonLdNamespace("https://example.com/test-profile/"), List.of("https://example.com/test-profile/context.jsonld"), List.of());
        var compactResult = serialize(typeTransformerRegistry, validatorRegistry, jsonLd, dataspaceProfile);

        assertThat(compactResult).isNotNull();
        assertThat(compactResult.getString(TYPE)).isEqualTo("DataspaceProfile");
        assertThat(compactResult.getString("name")).isEqualTo(dataspaceProfile.name());
        assertThat(compactResult.getJsonArray("jsonLdContextsUrl")).isEqualTo(createArrayBuilder(dataspaceProfile.jsonLdContextsUrl()).build());
        assertThat(compactResult.getJsonObject("protocol")).satisfies(p -> {
            assertThat(p.get("version")).isEqualTo(createValue(protocol.version()));
            assertThat(p.get("binding")).isEqualTo(createValue(protocol.binding()));
            assertThat(p.get("path")).isEqualTo(createValue(protocol.path()));
            assertThat(p.get("namespace")).isEqualTo(createValue(dataspaceProfile.protocolNamespace().namespace()));
        });
    }

    @Test
    void de_AssociateDataspaceProfileContext(TypeTransformerRegistry typeTransformerRegistry, JsonObjectValidatorRegistry validatorRegistry, JsonLd jsonLd) {

        var request = associateDataspaceProfileObject(jsonLdContext());
        var deserialized = deserialize(typeTransformerRegistry, validatorRegistry, jsonLd, request, AssociateDataspaceProfileContext.class);

        assertThat(deserialized.profiles()).contains("profile1", "profile2");
    }

    @Test
    void serde_DcpScope(TypeTransformerRegistry typeTransformerRegistry, JsonObjectValidatorRegistry validatorRegistry, JsonLd jsonLd) {
        verifySerde(typeTransformerRegistry, validatorRegistry, jsonLd, dcpScopeObject(jsonLdContext()), DcpScope.class, null);
    }

    @Test
    void serde_DcpScope_policy(TypeTransformerRegistry typeTransformerRegistry, JsonObjectValidatorRegistry validatorRegistry, JsonLd jsonLd) {
        verifySerde(typeTransformerRegistry, validatorRegistry, jsonLd, dcpScopePolicyObject(jsonLdContext()), DcpScope.class, null);
    }
}
