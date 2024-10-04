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

package org.eclipse.edc.test.e2e.managementapi;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry;
import org.eclipse.edc.policy.engine.spi.PolicyValidatorFunction;
import org.eclipse.edc.policy.engine.spi.plan.PolicyEvaluationPlan;
import org.eclipse.edc.policy.engine.spi.plan.step.AndConstraintStep;
import org.eclipse.edc.policy.engine.spi.plan.step.AtomicConstraintStep;
import org.eclipse.edc.policy.engine.spi.plan.step.ConstraintStep;
import org.eclipse.edc.policy.engine.spi.plan.step.DutyStep;
import org.eclipse.edc.policy.engine.spi.plan.step.OrConstraintStep;
import org.eclipse.edc.policy.engine.spi.plan.step.PermissionStep;
import org.eclipse.edc.policy.engine.spi.plan.step.ProhibitionStep;
import org.eclipse.edc.policy.engine.spi.plan.step.ValidatorStep;
import org.eclipse.edc.policy.engine.spi.plan.step.XoneConstraintStep;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.PolicyType;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static java.util.Collections.emptySet;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.REQUESTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.Type.CONSUMER;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.policy.model.Operator.EQ;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_CONNECTOR_MANAGEMENT_CONTEXT;
import static org.eclipse.edc.spi.types.domain.callback.CallbackAddress.EVENTS;
import static org.eclipse.edc.spi.types.domain.callback.CallbackAddress.IS_TRANSACTIONAL;
import static org.eclipse.edc.spi.types.domain.callback.CallbackAddress.URI;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestFunctions {

    public static final String TEST_ASSET_ID = "some-asset-id";
    public static final String TEST_ASSET_NAME = "some-asset-name";
    public static final String TEST_ASSET_DESCRIPTION = "some description";
    public static final String TEST_ASSET_CONTENTTYPE = "application/json";
    public static final String TEST_ASSET_VERSION = "0.2.1";
    public static final String MANAGEMENT_API_SCOPE = "MANAGEMENT_API";
    public static final String MANAGEMENT_API_CONTEXT = "management-api";
    private static final String DEFINITION_ID = "some-definition-id";
    private static final String POLICY_ID = "some-policy-id";

    public static JsonArrayBuilder createContextBuilder() {
        return createArrayBuilder()
                .add(EDC_CONNECTOR_MANAGEMENT_CONTEXT);
    }

    public static JsonObject assetObject() {
        return createObjectBuilder()
                .add(CONTEXT, createContextBuilder().build())
                .add(TYPE, "Asset")
                .add(ID, TEST_ASSET_ID)
                .add("properties", createObjectBuilder()
                        .add("name", TEST_ASSET_NAME)
                        .add("id", TEST_ASSET_ID)
                        .add("description", TEST_ASSET_DESCRIPTION)
                        .add("version", TEST_ASSET_VERSION)
                        .add("contenttype", TEST_ASSET_CONTENTTYPE)
                        .build())
                .add("dataAddress", createObjectBuilder().add("@type", "DataAddress").add("type", "address-type"))
                .build();
    }

    public static JsonObject dataAddressObject() {
        return createObjectBuilder()
                .add(CONTEXT, createContextBuilder().build())
                .add(TYPE, "DataAddress")
                .add("type", "address-type")
                .add("propertyOne", "foo")
                .add("propertyTwo", "bar")
                .build();
    }

    public static JsonObject contractDefinitionObject() {
        var criterion = Json.createObjectBuilder()
                .add(TYPE, "Criterion")
                .add("operandLeft", "foo")
                .add("operator", "=")
                .add("operandRight", "bar")
                .build();

        var assetsSelectorJson = createArrayBuilder()
                .add(criterion)
                .build();

        return createObjectBuilder()
                .add(CONTEXT, createContextBuilder().build())
                .add(TYPE, "ContractDefinition")
                .add(ID, DEFINITION_ID)
                .add("accessPolicyId", "accessPolicyId")
                .add("contractPolicyId", "contractPolicyId")
                .add("assetsSelector", assetsSelectorJson)
                .build();

    }

    public static JsonObject terminateNegotiationObject() {
        return Json.createObjectBuilder()
                .add(CONTEXT, createContextBuilder().build())
                .add(TYPE, "TerminateNegotiation")
                .add("reason", "reason")
                .build();
    }

    public static JsonObject terminateTransferObject() {
        return Json.createObjectBuilder()
                .add(CONTEXT, createContextBuilder().build())
                .add(TYPE, "TerminateTransfer")
                .add("reason", "reason")
                .build();
    }

    public static JsonObject suspendTransferObject() {
        return Json.createObjectBuilder()
                .add(CONTEXT, createContextBuilder().build())
                .add(TYPE, "SuspendTransfer")
                .add("reason", "reason")
                .build();
    }

    public static JsonObject contractRequestObject() {
        var policy = policy(atomicConstraint("spatial", "eq", "https://www.wikidata.org/wiki/Q183"))
                .add(ID, "id")
                .add("assigner", "provider")
                .build();
        return Json.createObjectBuilder()
                .add(CONTEXT, createContextBuilder().build())
                .add(TYPE, "ContractRequest")
                .add("counterPartyAddress", "test-address")
                .add("protocol", "test-protocol")
                .add("callbackAddresses", createCallbackAddress())
                .add("policy", policy)
                .build();
    }

    public static JsonObject transferRequestObject() {
        var dataDestination = createObjectBuilder()
                .add(TYPE, "DataAddress")
                .add("type", "type").build();

        return transferRequestObject(dataDestination);
    }

    public static JsonObject transferRequestObject(JsonObject dataDestination) {
        var propertiesJson = Json.createObjectBuilder().add("foo", "bar").build();
        var privatePropertiesJson = Json.createObjectBuilder().add("fooPrivate", "bar").build();


        return createObjectBuilder()
                .add(TYPE, "TransferRequest")
                .add(CONTEXT, createContextBuilder().build())
                .add(ID, "id")
                .add("counterPartyAddress", "address")
                .add("contractId", "contractId")
                .add("dataDestination", dataDestination)
                .add("properties", propertiesJson)
                .add("privateProperties", privatePropertiesJson)
                .add("protocol", "protocol")
                .add("callbackAddresses", createCallbackAddress())
                .add("transferType", "myTransferType")
                .build();
    }

    public static ContractAgreement createContractAgreement(String id) {
        return ContractAgreement.Builder.newInstance()
                .id(id)
                .providerId("providerId")
                .consumerId("consumerId")
                .assetId("assetId")
                .policy(Policy.Builder.newInstance().type(PolicyType.CONTRACT).build())
                .build();
    }

    public static ContractNegotiation createContractNegotiation() {
        return ContractNegotiation.Builder.newInstance()
                .id("test-id")
                .correlationId("correlation-id")
                .counterPartyId("counter-party-id")
                .counterPartyAddress("address")
                .contractAgreement(createContractAgreement("test-agreement"))
                .state(REQUESTED.code())
                .type(ContractNegotiation.Type.PROVIDER)
                .callbackAddresses(List.of(
                        CallbackAddress.Builder.newInstance()
                                .uri("local://test")
                                .events(Set.of("event"))
                                .build()))
                .protocol("protocol")
                .errorDetail("errorDetail")
                .createdAt(1234)
                .build();

    }

    public static TransferProcess createTransferProcess() {
        return TransferProcess.Builder.newInstance()
                .id("transferProcessId")
                .state(STARTED.code())
                .stateTimestamp(1234L)
                .privateProperties(Map.of("foo", "bar"))
                .type(CONSUMER)
                .correlationId("correlationId")
                .assetId("assetId")
                .contractId("contractId")
                .dataDestination(DataAddress.Builder.newInstance().type("any").properties(Map.of("bar", "foo")).build())
                .callbackAddresses(List.of(CallbackAddress.Builder.newInstance().uri("http://any").events(emptySet()).build()))
                .errorDetail("an error")
                .build();
    }

    public static EndpointDataReferenceEntry createEdrEntry() {
        return EndpointDataReferenceEntry.Builder.newInstance()
                .id("transferProcessId")
                .transferProcessId("transferProcessId")
                .agreementId("agreementId")
                .assetId("assetId")
                .providerId("providerId")
                .contractNegotiationId("contractNegotiationId")
                .createdAt(Clock.systemUTC().millis())
                .build();
    }

    public static PolicyEvaluationPlan createPolicyEvaluationPlan() {

        var firstConstraint = new AtomicConstraintStep(atomicConstraint("foo", "bar"), List.of("filtered constraint"),
                mock(), "AtomicConstraintFunction");
        var secondConstraint = new AtomicConstraintStep(atomicConstraint("baz", "bar"), List.of("filtered constraint"),
                mock(), "AtomicConstraintFunction");

        List<ConstraintStep> constraints = List.of(firstConstraint, secondConstraint);

        var orConstraintStep = new OrConstraintStep(constraints, mock());
        var andConstraintStep = new AndConstraintStep(constraints, mock());
        var xoneConstraintStep = new XoneConstraintStep(constraints, mock());

        var permission = PermissionStep.Builder.newInstance().constraint(orConstraintStep).rule(mock()).build();
        var duty = DutyStep.Builder.newInstance().constraint(xoneConstraintStep).rule(mock()).build();
        var prohibition = ProhibitionStep.Builder.newInstance().constraint(andConstraintStep).rule(mock()).build();

        var validatorFunction = mock(PolicyValidatorFunction.class);
        when(validatorFunction.name()).thenReturn("FunctionName");

        return PolicyEvaluationPlan.Builder.newInstance()
                .postValidator(new ValidatorStep(validatorFunction))
                .prohibition(prohibition)
                .permission(permission)
                .duty(duty)
                .preValidator(new ValidatorStep(validatorFunction))
                .build();
    }

    public static JsonObject policyDefinitionObject() {
        return createObjectBuilder()
                .add(CONTEXT, createContextBuilder().build())
                .add(TYPE, "PolicyDefinition")
                .add(ID, POLICY_ID)
                .add("policy", inForceDatePolicy("gteq", "contractAgreement+0s", "lteq", "contractAgreement+10s"))
                .build();
    }

    public static JsonObject policyDefinitionObject(JsonObject permission) {
        return createObjectBuilder()
                .add(CONTEXT, createContextBuilder().build())
                .add(TYPE, "PolicyDefinition")
                .add(ID, POLICY_ID)
                .add("policy", policy(permission))
                .build();
    }

    public static JsonObject secretObject() {
        return createObjectBuilder()
                .add(CONTEXT, createContextBuilder().build())
                .add(TYPE, "Secret")
                .add(ID, "secret-id")
                .add("value", "superSecret")
                .build();
    }

    private static JsonArrayBuilder createCallbackAddress() {
        var builder = Json.createArrayBuilder();
        return builder.add(Json.createObjectBuilder()
                .add(IS_TRANSACTIONAL, true)
                .add(URI, "http://test.local/")
                .add(EVENTS, Json.createArrayBuilder().build()));
    }

    public static JsonObjectBuilder policy(JsonObject permission) {
        return createObjectBuilder()
                .add(TYPE, "Set")
                .add("obligation", createArrayBuilder().build())
                .add("permission", permission)
                .add("target", "assetId")
                .add("prohibition", createArrayBuilder().build());
    }

    public static JsonObject querySpecObject() {
        var criterion = createObjectBuilder()
                .add(TYPE, "Criterion")
                .add("operandLeft", "foo")
                .add("operator", "=")
                .add("operandRight", "bar")
                .build();

        return createObjectBuilder()
                .add(CONTEXT, createContextBuilder().build())
                .add(TYPE, "QuerySpec")
                .add("offset", 10)
                .add("limit", 20)
                .add("filterExpression", createArrayBuilder().add(criterion).build())
                .add("sortOrder", "DESC")
                .add("sortField", "fieldName")
                .build();

    }

    public static JsonObject inForceDatePolicy(String operatorStart, Object startDate, String operatorEnd, Object endDate) {
        return policy(inForceDatePermission(operatorStart, startDate, operatorEnd, endDate)).build();
    }

    public static JsonObject atomicConstraint(String leftOperand, String operator, Object rightOperand) {
        return createObjectBuilder()
                .add("leftOperand", leftOperand)
                .add("operator", operator)
                .add("rightOperand", rightOperand.toString())
                .build();
    }

    public static JsonObject inForceDatePermission(String operatorStart, Object startDate, String operatorEnd, Object endDate) {
        return createObjectBuilder()
                .add("action", "use")
                .add("constraint", createObjectBuilder()
                        .add("and", createArrayBuilder()
                                .add(atomicConstraint("inForceDate", operatorStart, startDate))
                                .add(atomicConstraint("inForceDate", operatorEnd, endDate))
                                .build())
                        .build())
                .build();
    }

    public static JsonObject policyEvaluationPlanRequest() {
        return Json.createObjectBuilder()
                .add(CONTEXT, createContextBuilder().build())
                .add(TYPE, "PolicyEvaluationPlanRequest")
                .add("policyScope", "catalog")
                .build();
    }

    private static AtomicConstraint atomicConstraint(String key, String value) {
        var left = new LiteralExpression(key);
        var right = new LiteralExpression(value);
        return AtomicConstraint.Builder.newInstance()
                .leftExpression(left)
                .operator(EQ)
                .rightExpression(right)
                .build();
    }
}
