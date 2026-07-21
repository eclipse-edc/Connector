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
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import org.eclipse.edc.api.model.IdResponse;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequest;
import org.eclipse.edc.connector.controlplane.catalog.spi.DatasetRequest;
import org.eclipse.edc.connector.controlplane.contract.spi.types.command.TerminateNegotiationCommand;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequest;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.NegotiationState;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.TerminateNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.controlplane.dataplane.spi.instance.DataPlaneInstance;
import org.eclipse.edc.connector.controlplane.dataplane.spi.instance.DataPlaneInstanceStates;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyEvaluationPlanRequest;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyValidationResult;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.SuspendTransfer;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TerminateTransfer;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferRequest;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferState;
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
import org.eclipse.edc.iam.decentralizedclaims.spi.scope.DcpScopeRegistry;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.ComponentRuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.participantcontext.spi.config.model.ParticipantContextConfiguration;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.policy.cel.model.CelExpression;
import org.eclipse.edc.policy.model.AndConstraint;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.secret.Secret;
import org.eclipse.edc.test.e2e.managementapi.Runtimes;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.support.AnnotationConsumer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.REQUESTED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.MANAGEMENT_API_CONTEXT;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.MANAGEMENT_API_SCOPE;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.assetObject;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.assetObjectWithMetadata;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.catalogAsset;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.catalogRequestObject;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.catalogRequestObjectWithProfile;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.catalogRequestObjectWithProfileAndProtocol;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.celExpression;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.contractDefinitionObject;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.contractRequestObject;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.contractRequestObjectWithProfile;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.contractRequestObjectWithProfileAndProtocol;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.createContractAgreement;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.createContractNegotiation;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.createPolicyEvaluationPlan;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.createTransferProcess;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.dataAddressObject;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.datasetRequestObject;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.datasetRequestObjectWithProfile;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.datasetRequestObjectWithProfileAndProtocol;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.inForceDatePermission;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.participantContextConfigObject;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.participantContextObject;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.policyDefinitionObject;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.policyEvaluationPlanRequest;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.querySpecObject;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.secretObject;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.suspendTransferObject;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.terminateNegotiationObject;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.terminateTransferObject;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.transferRequestObject;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.transferRequestObjectWithProfile;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.transferRequestObjectWithProfileAndProtocol;
import static org.mockito.Mockito.mock;

@EndToEndTest
public class SerdeV4EndToEndTest extends SerdeTestBase {

    @RegisterExtension
    static RuntimeExtension runtime = ComponentRuntimeExtension.Builder.newInstance()
            .name(Runtimes.ControlPlane.NAME)
            .modules(Runtimes.ControlPlane.MODULES)
            .modules(":extensions:common:api:management-api-schema-validator")
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
        return "v4";
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
    void ser_ContractAgreement(TypeTransformerRegistry typeTransformerRegistry, JsonObjectValidatorRegistry validatorRegistry, JsonLd jsonLd) {
        var agreement = createContractAgreement("test-id");
        var compactResult = serialize(typeTransformerRegistry, validatorRegistry, jsonLd, agreement);

        assertThat(compactResult).isNotNull();
        assertThat(compactResult.getString(ID)).isEqualTo(agreement.getId());
        assertThat(compactResult.getString(TYPE)).isEqualTo("ContractAgreement");
        assertThat(compactResult.getString("providerId")).isEqualTo(agreement.getProviderId());
        assertThat(compactResult.getString("consumerId")).isEqualTo(agreement.getConsumerId());
        assertThat(compactResult.getString("assetId")).isEqualTo(agreement.getAssetId());
        assertThat(compactResult.getString("agreementId")).isEqualTo(agreement.getAgreementId());
        assertThat(compactResult.getJsonObject("policy")).isNotNull()
                .satisfies(policy -> {
                    assertThat(policy.get(TYPE)).isEqualTo(Json.createValue("Agreement"));
                });
        assertThat(compactResult.getJsonNumber("contractSigningDate")).isNotNull();

    }

    @Test
    void ser_ContractNegotiation(TypeTransformerRegistry typeTransformerRegistry, JsonObjectValidatorRegistry validatorRegistry, JsonLd jsonLd) {
        var negotiation = createContractNegotiation();
        var compactResult = serialize(typeTransformerRegistry, validatorRegistry, jsonLd, negotiation);

        assertThat(compactResult).isNotNull();
        assertThat(compactResult.getString(ID)).isEqualTo("test-id");
        assertThat(compactResult.getString("state")).isEqualTo(REQUESTED.name());
        assertThat(compactResult.getString("counterPartyId")).isEqualTo(negotiation.getCounterPartyId());
        assertThat(compactResult.getString("counterPartyAddress")).isEqualTo(negotiation.getCounterPartyAddress());
        assertThat(compactResult.getString("contractAgreementId")).isEqualTo(negotiation.getContractAgreement().getId());
        assertThat(compactResult.getString("errorDetail")).isEqualTo(negotiation.getErrorDetail());
        assertThat(compactResult.getString("type")).isEqualTo(negotiation.getType().toString());
        assertThat(compactResult.getString("protocol")).isEqualTo(negotiation.getProtocol());
        assertThat(compactResult.getJsonArray("callbackAddresses")).isNotEmpty().first().satisfies(callback -> {
            var firstCallback = negotiation.getCallbackAddresses().get(0);
            var cb = callback.asJsonObject();
            assertThat(cb.getString("uri")).isEqualTo(firstCallback.getUri());
            assertThat(cb.getBoolean("transactional")).isEqualTo(firstCallback.isTransactional());

            var events = cb.getJsonArray("events").stream().map(event -> ((JsonString) event).getString()).collect(Collectors.toSet());
            assertThat(events).containsAll(firstCallback.getEvents());
        });
        assertThat(compactResult.getJsonNumber("createdAt").longValue()).isEqualTo(1234);
        assertThat(compactResult.getString("assetId")).isEqualTo(negotiation.getLastContractOffer().getAssetId());
        assertThat(compactResult.getString("correlationId")).isEqualTo(negotiation.getCorrelationId());

    }

    @Test
    void ser_TransferProcess(TypeTransformerRegistry typeTransformerRegistry, JsonObjectValidatorRegistry validatorRegistry, JsonLd jsonLd) {
        var transferProcess = createTransferProcess();
        var compactResult = serialize(typeTransformerRegistry, validatorRegistry, jsonLd, transferProcess);

        assertThat(compactResult).isNotNull();
        assertThat(compactResult.getString(ID)).isEqualTo(transferProcess.getId());
        assertThat(compactResult.getString(TYPE)).isEqualTo("TransferProcess");
        assertThat(compactResult.getString("correlationId")).isEqualTo(transferProcess.getCorrelationId());
        assertThat(compactResult.getString("state")).isEqualTo(transferProcess.stateAsString());
        assertThat(compactResult.getJsonNumber("stateTimestamp").longValue()).isEqualTo(transferProcess.getStateTimestamp());
        assertThat(compactResult.getString("assetId")).isEqualTo(transferProcess.getAssetId());
        assertThat(compactResult.getString("contractId")).isEqualTo(transferProcess.getContractId());
        assertThat(compactResult.getString("type")).isEqualTo(transferProcess.getType().toString());
        assertThat(compactResult.getString("transferType")).isEqualTo(transferProcess.getTransferType());
        assertThat(compactResult.getJsonObject("dataDestination")).isNotNull();
        assertThat(compactResult.getJsonArray("callbackAddresses")).hasSize(transferProcess.getCallbackAddresses().size());
        assertThat(compactResult.getString("errorDetail")).isEqualTo(transferProcess.getErrorDetail());
    }

    @Test
    void ser_NegotiationState(TypeTransformerRegistry typeTransformerRegistry, JsonObjectValidatorRegistry validatorRegistry, JsonLd jsonLd) {
        var state = new NegotiationState(TransferProcessStates.REQUESTED.name());
        var compactResult = serialize(typeTransformerRegistry, validatorRegistry, jsonLd, state);

        assertThat(compactResult).isNotNull();
        assertThat(compactResult.getString(TYPE)).isEqualTo("NegotiationState");
        assertThat(compactResult.getString("state")).isEqualTo(TransferProcessStates.REQUESTED.name());

    }

    @Test
    void ser_TransferState(TypeTransformerRegistry typeTransformerRegistry, JsonObjectValidatorRegistry validatorRegistry, JsonLd jsonLd) {
        var state = new TransferState(REQUESTED.name());
        var compactResult = serialize(typeTransformerRegistry, validatorRegistry, jsonLd, state);

        assertThat(compactResult).isNotNull();
        assertThat(compactResult.getString(TYPE)).isEqualTo("TransferState");
        assertThat(compactResult.getString("state")).isEqualTo(REQUESTED.name());

    }

    @Test
    void ser_DataPlaneInstance(TypeTransformerRegistry typeTransformerRegistry, JsonObjectValidatorRegistry validatorRegistry, JsonLd jsonLd) {
        var instance = DataPlaneInstance.Builder.newInstance().id("id")
                .state(DataPlaneInstanceStates.REGISTERED.code())
                .url("http://localhost:8080")
                .allowedSourceType("sourceType")
                .allowedTransferType("transferType")
                .destinationProvisionTypes(Set.of("provisionType"))
                .property(EDC_NAMESPACE + "custom", "value")
                .build();

        var compactResult = serialize(typeTransformerRegistry, validatorRegistry, jsonLd, instance);

        assertThat(compactResult).isNotNull();
        assertThat(compactResult.getString(TYPE)).isEqualTo("DataPlaneInstance");
        assertThat(compactResult.getString(ID)).isEqualTo(instance.getId());
        assertThat(compactResult.getJsonArray("allowedSourceTypes")).hasSize(1);
        assertThat(compactResult.getJsonArray("allowedTransferTypes")).hasSize(1);
        assertThat(compactResult.getJsonArray("destinationProvisionTypes")).hasSize(1);
        assertThat(compactResult.getJsonObject("properties")).hasSize(1);

    }

    @Test
    void ser_PolicyValidationResult(TypeTransformerRegistry typeTransformerRegistry, JsonObjectValidatorRegistry validatorRegistry, JsonLd jsonLd) {
        var result = new PolicyValidationResult(false, List.of("error1", "error2"));
        var compactResult = serialize(typeTransformerRegistry, validatorRegistry, jsonLd, result);

        assertThat(compactResult).isNotNull();
        assertThat(compactResult.getString(TYPE)).isEqualTo("PolicyValidationResult");
        assertThat(compactResult.getBoolean("isValid")).isEqualTo(false);
        assertThat(compactResult.getJsonArray("errors")).contains(Json.createValue("error1"), Json.createValue("error2"));

    }

    @Test
    void ser_PolicyEvaluationPlan(TypeTransformerRegistry typeTransformerRegistry, JsonObjectValidatorRegistry validatorRegistry, JsonLd jsonLd) {
        var plan = createPolicyEvaluationPlan();
        var compactResult = serialize(typeTransformerRegistry, validatorRegistry, jsonLd, plan);

        assertThat(compactResult).isNotNull();
        assertThat(compactResult.getString(TYPE)).isEqualTo("PolicyEvaluationPlan");
        assertThat(compactResult.getJsonArray("preValidators")).hasSize(1);
        assertThat(compactResult.getJsonArray("postValidators")).hasSize(1);


        BiFunction<String, String, Consumer<JsonValue>> ruleAsser = (ruleType, multiplicityType) -> (ruleValue) -> {
            var rule = ruleValue.asJsonObject();
            assertThat(rule.getString(TYPE)).isEqualTo(ruleType);
            assertThat(rule.getBoolean("isFiltered")).isFalse();
            assertThat(rule.getJsonArray("filteringReasons")).hasSize(0);
            assertThat(rule.getJsonArray("ruleFunctions")).hasSize(0);
            assertThat(rule.getJsonArray("constraintSteps")).hasSize(1);

            var constraint = rule.getJsonArray("constraintSteps").get(0).asJsonObject();
            assertThat(constraint.getString(TYPE)).isEqualTo(multiplicityType);
            assertThat(constraint.getJsonArray("constraintSteps")).hasSize(2)
                    .allSatisfy(value -> {
                        var atomicConstraint = value.asJsonObject();
                        assertThat(atomicConstraint.getString(TYPE)).isEqualTo("AtomicConstraintStep");
                        assertThat(atomicConstraint.getJsonArray("filteringReasons")).hasSize(1);
                        assertThat(atomicConstraint.getBoolean("isFiltered")).isTrue();
                        assertThat(atomicConstraint.getString("functionName")).isEqualTo("AtomicConstraintFunction");
                        assertThat(atomicConstraint.getJsonArray("functionParams")).hasSize(3);
                    });
        };

        assertThat(compactResult.getJsonArray("permissionSteps")).hasSize(1)
                .first()
                .satisfies(ruleAsser.apply("PermissionStep", "OrConstraintStep"));

        assertThat(compactResult.getJsonArray("prohibitionSteps")).hasSize(1)
                .first()
                .satisfies(ruleAsser.apply("ProhibitionStep", "AndConstraintStep"));

        assertThat(compactResult.getJsonArray("obligationSteps")).hasSize(1)
                .first()
                .satisfies(ruleAsser.apply("DutyStep", "XoneConstraintStep"));

    }

    @Test
    void ser_IdResponse(TypeTransformerRegistry typeTransformerRegistry, JsonObjectValidatorRegistry validatorRegistry, JsonLd jsonLd) {
        var response = IdResponse.Builder.newInstance().id("test-id").createdAt(1234).build();
        var compactResult = serialize(typeTransformerRegistry, validatorRegistry, jsonLd, response);

        assertThat(compactResult).isNotNull();
        assertThat(compactResult.getString(TYPE)).isEqualTo("IdResponse");
        assertThat(compactResult.getString(ID)).isEqualTo(response.getId());
        assertThat(compactResult.getInt("createdAt")).isEqualTo(response.getCreatedAt());
    }

    @Test
    void de_DatasetRequest(TypeTransformerRegistry typeTransformerRegistry, JsonObjectValidatorRegistry validatorRegistry, JsonLd jsonLd) {
        var inputObject = datasetRequestObject(jsonLdContext());
        var request = deserialize(typeTransformerRegistry, validatorRegistry, jsonLd, inputObject, DatasetRequest.class);

        assertThat(request).isNotNull();
        assertThat(request.getId()).isEqualTo(inputObject.getString(ID));
        assertThat(request.getProtocol()).isEqualTo(inputObject.getString("protocol"));
        assertThat(request.getCounterPartyAddress()).isEqualTo(inputObject.getString("counterPartyAddress"));
        assertThat(request.getCounterPartyId()).isEqualTo(inputObject.getString("counterPartyId"));

    }

    @Test
    void de_DatasetRequest_withProfile(TypeTransformerRegistry typeTransformerRegistry, JsonObjectValidatorRegistry validatorRegistry, JsonLd jsonLd) {
        var inputObject = datasetRequestObjectWithProfile(jsonLdContext());
        var request = deserialize(typeTransformerRegistry, validatorRegistry, jsonLd, inputObject, DatasetRequest.class);

        assertThat(request).isNotNull();
        assertThat(request.getId()).isEqualTo(inputObject.getString(ID));
        assertThat(request.getProfile()).isEqualTo(inputObject.getString("profile"));
        assertThat(request.getCounterPartyAddress()).isEqualTo(inputObject.getString("counterPartyAddress"));
        assertThat(request.getCounterPartyId()).isEqualTo(inputObject.getString("counterPartyId"));

    }

    @Test
    void validate_DatasetRequest_WithBoth_ShouldFail(JsonObjectValidatorRegistry validatorRegistry) {
        var inputObject = datasetRequestObjectWithProfileAndProtocol(jsonLdContext());
        var request = validateWithResult(validatorRegistry, inputObject);

        assertThat(request).isFailed();

    }

    @Test
    void de_CatalogRequest(TypeTransformerRegistry typeTransformerRegistry, JsonObjectValidatorRegistry validatorRegistry, JsonLd jsonLd) {
        var inputObject = catalogRequestObject(jsonLdContext());
        var request = deserialize(typeTransformerRegistry, validatorRegistry, jsonLd, inputObject, CatalogRequest.class);

        assertThat(request).isNotNull();
        assertThat(request.getProtocol()).isEqualTo(inputObject.getString("protocol"));
        assertThat(request.getCounterPartyAddress()).isEqualTo(inputObject.getString("counterPartyAddress"));
        assertThat(request.getCounterPartyId()).isEqualTo(inputObject.getString("counterPartyId"));
        assertThat(request.getQuerySpec().getFilterExpression()).hasSize(1);

    }

    @Test
    void de_CatalogRequest_WithProfile(TypeTransformerRegistry typeTransformerRegistry, JsonObjectValidatorRegistry validatorRegistry, JsonLd jsonLd) {
        var inputObject = catalogRequestObjectWithProfile(jsonLdContext());
        var request = deserialize(typeTransformerRegistry, validatorRegistry, jsonLd, inputObject, CatalogRequest.class);

        assertThat(request).isNotNull();
        assertThat(request.getProfile()).isEqualTo(inputObject.getString("profile"));
        assertThat(request.getCounterPartyAddress()).isEqualTo(inputObject.getString("counterPartyAddress"));
        assertThat(request.getCounterPartyId()).isEqualTo(inputObject.getString("counterPartyId"));
        assertThat(request.getQuerySpec().getFilterExpression()).hasSize(1);

    }

    @Test
    void validate_CatalogRequest_WithBoth_ShouldFail(JsonObjectValidatorRegistry validatorRegistry) {
        var inputObject = catalogRequestObjectWithProfileAndProtocol(jsonLdContext());
        var request = validateWithResult(validatorRegistry, inputObject);

        assertThat(request).isFailed();

    }

    @Test
    void de_ContractRequest(TypeTransformerRegistry typeTransformerRegistry, JsonObjectValidatorRegistry validatorRegistry, JsonLd jsonLd) {
        var inputObject = contractRequestObject(jsonLdContext(), strictSchema());
        var request = deserialize(typeTransformerRegistry, validatorRegistry, jsonLd, inputObject, ContractRequest.class);

        assertThat(request).isNotNull();
        assertThat(request.getProviderId()).isEqualTo(inputObject.getJsonObject("policy").getString("assigner"));
        assertThat(request.getCallbackAddresses()).isNotEmpty();
        assertThat(request.getProtocol()).isEqualTo("test-protocol");
        assertThat(request.getCounterPartyAddress()).isEqualTo("test-address");
        assertThat(request.getContractOffer().getPolicy()).isNotNull();

    }

    @Test
    void de_ContractRequest_withProfile(TypeTransformerRegistry typeTransformerRegistry, JsonObjectValidatorRegistry validatorRegistry, JsonLd jsonLd) {
        var inputObject = contractRequestObjectWithProfile(jsonLdContext(), strictSchema());
        var request = deserialize(typeTransformerRegistry, validatorRegistry, jsonLd, inputObject, ContractRequest.class);

        assertThat(request).isNotNull();
        assertThat(request.getProviderId()).isEqualTo(inputObject.getJsonObject("policy").getString("assigner"));
        assertThat(request.getCallbackAddresses()).isNotEmpty();
        assertThat(request.getProfile()).isEqualTo("test-profile");
        assertThat(request.getCounterPartyAddress()).isEqualTo("test-address");
        assertThat(request.getContractOffer().getPolicy()).isNotNull();

    }

    @Test
    void validate_ContractRequest_withProfileAndProtocol_shouldFail(JsonObjectValidatorRegistry validatorRegistry) {
        var inputObject = contractRequestObjectWithProfileAndProtocol(jsonLdContext(), strictSchema());
        var request = validateWithResult(validatorRegistry, inputObject);

        assertThat(request).isFailed();

    }

    @Test
    void de_TransferRequest(TypeTransformerRegistry typeTransformerRegistry, JsonObjectValidatorRegistry validatorRegistry, JsonLd jsonLd) {
        var inputObject = transferRequestObject(jsonLdContext());
        var transferRequest = deserialize(typeTransformerRegistry, validatorRegistry, jsonLd, inputObject, TransferRequest.class);

        assertThat(transferRequest).isNotNull();
        assertThat(transferRequest.getCounterPartyAddress()).isEqualTo(inputObject.getString("counterPartyAddress"));
        assertThat(transferRequest.getContractId()).isEqualTo(inputObject.getString("contractId"));
        assertThat(transferRequest.getDataDestination()).extracting(DataAddress::getType).isEqualTo(inputObject.getJsonObject("dataDestination").getString("type"));
        assertThat(transferRequest.getPrivateProperties()).containsEntry(EDC_NAMESPACE + "fooPrivate", "bar");
        assertThat(transferRequest.getProtocol()).isEqualTo(inputObject.getString("protocol"));
        assertThat(transferRequest.getCallbackAddresses()).hasSize(inputObject.getJsonArray("callbackAddresses").size());
        assertThat(transferRequest.getPrivateProperties()).hasSize(inputObject.getJsonObject("privateProperties").size());
        assertThat(transferRequest.getTransferType()).isEqualTo(inputObject.getString("transferType"));
    }

    @Test
    void de_TransferRequest_withProfile(TypeTransformerRegistry typeTransformerRegistry, JsonObjectValidatorRegistry validatorRegistry, JsonLd jsonLd) {
        var inputObject = transferRequestObjectWithProfile(jsonLdContext());
        var transferRequest = deserialize(typeTransformerRegistry, validatorRegistry, jsonLd, inputObject, TransferRequest.class);

        assertThat(transferRequest).isNotNull();
        assertThat(transferRequest.getCounterPartyAddress()).isEqualTo(inputObject.getString("counterPartyAddress"));
        assertThat(transferRequest.getContractId()).isEqualTo(inputObject.getString("contractId"));
        assertThat(transferRequest.getDataDestination()).extracting(DataAddress::getType).isEqualTo(inputObject.getJsonObject("dataDestination").getString("type"));
        assertThat(transferRequest.getPrivateProperties()).containsEntry(EDC_NAMESPACE + "fooPrivate", "bar");
        assertThat(transferRequest.getProfile()).isEqualTo(inputObject.getString("profile"));
        assertThat(transferRequest.getCallbackAddresses()).hasSize(inputObject.getJsonArray("callbackAddresses").size());
        assertThat(transferRequest.getPrivateProperties()).hasSize(inputObject.getJsonObject("privateProperties").size());
        assertThat(transferRequest.getTransferType()).isEqualTo(inputObject.getString("transferType"));
    }

    @Test
    void validate_TransferRequest_withProfileAndProtocol_shouldFail(JsonObjectValidatorRegistry validatorRegistry) {
        var inputObject = transferRequestObjectWithProfileAndProtocol(jsonLdContext());
        var request = validateWithResult(validatorRegistry, inputObject);

        assertThat(request).isFailed();
    }

    @Test
    void de_TerminateNegotiation(TypeTransformerRegistry typeTransformerRegistry, JsonObjectValidatorRegistry validatorRegistry, JsonLd jsonLd) {
        var inputObject = terminateNegotiationObject(jsonLdContext());
        var terminateNegotiation = deserialize(typeTransformerRegistry, validatorRegistry, jsonLd, inputObject, TerminateNegotiationCommand.class);

        assertThat(terminateNegotiation).isNotNull();
        assertThat(terminateNegotiation.getReason()).isEqualTo(inputObject.getString("reason"));
    }

    @Test
    void de_TerminateNegotiation_v4(TypeTransformerRegistry typeTransformerRegistry, JsonObjectValidatorRegistry validatorRegistry, JsonLd jsonLd) {
        var inputObject = terminateNegotiationObject(jsonLdContext());
        var terminateNegotiation = deserialize(typeTransformerRegistry, validatorRegistry, jsonLd, inputObject, TerminateNegotiation.class);

        assertThat(terminateNegotiation).isNotNull();
        assertThat(terminateNegotiation.reason()).isEqualTo(inputObject.getString("reason"));
    }

    @Test
    void de_TerminateTransfer(TypeTransformerRegistry typeTransformerRegistry, JsonObjectValidatorRegistry validatorRegistry, JsonLd jsonLd) {
        var inputObject = terminateTransferObject(jsonLdContext());
        var terminateTransfer = deserialize(typeTransformerRegistry, validatorRegistry, jsonLd, inputObject, TerminateTransfer.class);

        assertThat(terminateTransfer).isNotNull();
        assertThat(terminateTransfer.reason()).isEqualTo(inputObject.getString("reason"));

    }

    @Test
    void de_SuspendTransfer(TypeTransformerRegistry typeTransformerRegistry, JsonObjectValidatorRegistry validatorRegistry, JsonLd jsonLd) {
        var inputObject = suspendTransferObject(jsonLdContext());
        var terminateTransfer = deserialize(typeTransformerRegistry, validatorRegistry, jsonLd, inputObject, SuspendTransfer.class);

        assertThat(terminateTransfer).isNotNull();
        assertThat(terminateTransfer.reason()).isEqualTo(inputObject.getString("reason"));
    }

    @Test
    void de_PolicyDefinition_withInForceDate(TypeTransformerRegistry typeTransformerRegistry, JsonObjectValidatorRegistry validatorRegistry, JsonLd jsonLd) {

        var andConstraint = AndConstraint.Builder.newInstance()
                .constraint(AtomicConstraint.Builder.newInstance()
                        .leftExpression(new LiteralExpression(EDC_NAMESPACE + "inForceDate"))
                        .operator(Operator.GEQ)
                        .rightExpression(new LiteralExpression("contractAgreement+0s"))
                        .build())
                .constraint(AtomicConstraint.Builder.newInstance()
                        .leftExpression(new LiteralExpression(EDC_NAMESPACE + "inForceDate"))
                        .operator(Operator.LEQ)
                        .rightExpression(new LiteralExpression("contractAgreement+10s"))
                        .build())
                .build();

        var permission = inForceDatePermission("gteq", "contractAgreement+0s", "lteq", "contractAgreement+10s", strictSchema());
        var inputObject = policyDefinitionObject(jsonLdContext(), permission, strictSchema());

        var deserialized = deserialize(typeTransformerRegistry, validatorRegistry, jsonLd, inputObject, PolicyDefinition.class);

        assertThat(deserialized).isNotNull();
        assertThat(deserialized.getPolicy().getPermissions().get(0).getConstraints())
                .usingRecursiveFieldByFieldElementComparator().containsOnly(andConstraint);

    }

    @Test
    void de_PolicyEvaluationPlanRequest(TypeTransformerRegistry typeTransformerRegistry, JsonObjectValidatorRegistry validatorRegistry, JsonLd jsonLd) {
        var inputObject = policyEvaluationPlanRequest(jsonLdContext());
        var terminateTransfer = deserialize(typeTransformerRegistry, validatorRegistry, jsonLd, inputObject, PolicyEvaluationPlanRequest.class);

        assertThat(terminateTransfer).isNotNull();
        assertThat(terminateTransfer.policyScope()).isEqualTo(inputObject.getString("policyScope"));

    }


    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    @ArgumentsSource(JsonInputProvider.class)
    protected @interface WithContext {
        String value();

        boolean strictSchema() default false;
    }

    protected static class JsonInputProvider implements ArgumentsProvider, AnnotationConsumer<WithContext> {

        private String jsonLdContext;
        private boolean strictSchema;

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {

            Function<JsonObject, JsonObject> mapper = this::policyMapper;

            return Stream.of(
                    Arguments.of(assetObject(jsonLdContext), Asset.class, null),
                    Arguments.of(assetObjectWithMetadata(jsonLdContext), Asset.class, null),
                    Arguments.of(catalogAsset(jsonLdContext), Asset.class, null),
                    Arguments.of(contractDefinitionObject(jsonLdContext), ContractDefinition.class, null),
                    Arguments.of(secretObject(jsonLdContext), Secret.class, null),
                    Arguments.of(querySpecObject(jsonLdContext), QuerySpec.class, null),
                    Arguments.of(policyDefinitionObject(jsonLdContext, strictSchema), PolicyDefinition.class, mapper),
                    Arguments.of(dataAddressObject(jsonLdContext), DataAddress.class, null),
                    Arguments.of(participantContextObject(jsonLdContext), ParticipantContext.class, null),
                    Arguments.of(participantContextConfigObject(jsonLdContext), ParticipantContextConfiguration.class, null),
                    Arguments.of(celExpression(jsonLdContext), CelExpression.class, null)
            );
        }

        private JsonObject policyMapper(JsonObject compacted) {
            var policy = compacted.getJsonObject("policy");
            var newPolicy = Json.createObjectBuilder(policy);
            newPolicy.remove("@id");
            var newDefinition = Json.createObjectBuilder(compacted);
            newDefinition.remove("createdAt");
            newDefinition.add("policy", newPolicy);

            return newDefinition.build();
        }

        @Override
        public void accept(WithContext withContext) {
            jsonLdContext = withContext.value();
            strictSchema = withContext.strictSchema();
        }
    }

}
