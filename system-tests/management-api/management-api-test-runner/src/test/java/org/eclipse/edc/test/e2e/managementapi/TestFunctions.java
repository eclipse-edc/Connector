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
import org.eclipse.edc.policy.engine.spi.PolicyValidatorRule;
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
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.policy.model.Permission;
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

    public static JsonArrayBuilder createContextBuilder(String context) {
        return createArrayBuilder()
                .add(context);
    }

    public static JsonObject assetObject(String context) {
        return createObjectBuilder()
                .add(CONTEXT, createContextBuilder(context).build())
                .add(TYPE, "Asset")
                .add(ID, TEST_ASSET_ID)
                .add("properties", createObjectBuilder()
                        .add("name", TEST_ASSET_NAME)
                        .add("id", TEST_ASSET_ID)
                        .add("description", TEST_ASSET_DESCRIPTION)
                        .add("version", TEST_ASSET_VERSION)
                        .add("contenttype", TEST_ASSET_CONTENTTYPE)
                        .build())
                .add("privateProperties", createObjectBuilder()
                        .add("name", TEST_ASSET_NAME)
                        .add("id", TEST_ASSET_ID)
                        .add("description", TEST_ASSET_DESCRIPTION)
                        .add("version", TEST_ASSET_VERSION)
                        .add("contenttype", TEST_ASSET_CONTENTTYPE)
                        .build())
                .add("dataAddress", createObjectBuilder().add("@type", "DataAddress").add("type", "address-type"))
                .build();
    }

    public static JsonObject dataAddressObject(String context) {
        return createObjectBuilder()
                .add(CONTEXT, createContextBuilder(context).build())
                .add(TYPE, "DataAddress")
                .add("type", "address-type")
                .add("propertyOne", "foo")
                .add("propertyTwo", "bar")
                .build();
    }

    public static JsonObject dataPaneInstanceObject(String context) {
        return createObjectBuilder()
                .add(CONTEXT, createContextBuilder(context).build())
                .add(TYPE, "DataPlaneInstance")
                .add(ID, "data-plane-instance-id")
                .add("url", "http://test.local/")
                .add("lastActive", 1234567890L)
                .add("allowedSourceTypes", createArrayBuilder().add("Source"))
                .add("allowedTransferTypes", createArrayBuilder().add("TransferType"))
                .add("destinationProvisionTypes", createArrayBuilder().add("ProvisionType"))
                .add("stateTimestamp", 1234567890L)
                .build();
    }

    public static JsonObject contractDefinitionObject(String context) {
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
                .add(CONTEXT, createContextBuilder(context).build())
                .add(TYPE, "ContractDefinition")
                .add(ID, DEFINITION_ID)
                .add("accessPolicyId", "accessPolicyId")
                .add("contractPolicyId", "contractPolicyId")
                .add("assetsSelector", assetsSelectorJson)
                .add("privateProperties", createObjectBuilder()
                        .add("name", "contract-definition-name")
                        .add("id", DEFINITION_ID)
                        .add("description", "contract definition description")
                        .build())
                .build();

    }

    public static JsonObject terminateNegotiationObject(String context) {
        return Json.createObjectBuilder()
                .add(CONTEXT, createContextBuilder(context).build())
                .add(TYPE, "TerminateNegotiation")
                .add("reason", "reason")
                .build();
    }

    public static JsonObject terminateTransferObject(String context) {
        return Json.createObjectBuilder()
                .add(CONTEXT, createContextBuilder(context).build())
                .add(TYPE, "TerminateTransfer")
                .add("reason", "reason")
                .build();
    }

    public static JsonObject suspendTransferObject(String context) {
        return Json.createObjectBuilder()
                .add(CONTEXT, createContextBuilder(context).build())
                .add(TYPE, "SuspendTransfer")
                .add("reason", "reason")
                .build();
    }

    public static JsonObject contractRequestObject(String context, boolean alwaysArray) {
        var permission = inForceDatePermission("gteq", "contractAgreement+0s", "lteq", "contractAgreement+10s", alwaysArray);
        var policy = policy(permission, "Offer", alwaysArray)
                .add(ID, "id")
                .add("assigner", "provider")
                .build();
        return Json.createObjectBuilder()
                .add(CONTEXT, createContextBuilder(context).build())
                .add(TYPE, "ContractRequest")
                .add("counterPartyAddress", "test-address")
                .add("protocol", "test-protocol")
                .add("callbackAddresses", createCallbackAddress())
                .add("policy", policy)
                .build();
    }

    public static JsonObject datasetRequestObject(String context) {
        return Json.createObjectBuilder()
                .add(CONTEXT, createContextBuilder(context).build())
                .add(TYPE, "DatasetRequest")
                .add(ID, "dataset-request-id")
                .add("counterPartyAddress", "test-address")
                .add("counterPartyId", "test-counter-party-id")
                .add("protocol", "test-protocol")
                .build();
    }

    public static JsonObject catalogRequestObject(String context) {
        return Json.createObjectBuilder()
                .add(CONTEXT, createContextBuilder(context).build())
                .add(TYPE, "CatalogRequest")
                .add("counterPartyAddress", "test-address")
                .add("counterPartyId", "test-counter-party-id")
                .add("protocol", "test-protocol")
                .add("querySpec", embeddedQuerySpec())
                .build();
    }

    public static JsonObject transferRequestObject(String context) {
        var dataDestination = createObjectBuilder()
                .add(TYPE, "DataAddress")
                .add("type", "type").build();

        return transferRequestObject(context, dataDestination);
    }

    public static JsonObject transferRequestObject(String context, JsonObject dataDestination) {
        var propertiesJson = Json.createObjectBuilder().add("foo", "bar").build();
        var privatePropertiesJson = Json.createObjectBuilder().add("fooPrivate", "bar").build();


        return createObjectBuilder()
                .add(TYPE, "TransferRequest")
                .add(CONTEXT, createContextBuilder(context).build())
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
                .policy(Policy.Builder.newInstance().type(PolicyType.CONTRACT)
                        .target("assetId")
                        .assignee("providerId")
                        .assigner("consumerId")
                        .permission(Permission.Builder.newInstance().action(Action.Builder.newInstance().type("use").build()).build())
                        .build())
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
                .transferType("myTransferType")
                .dataDestination(DataAddress.Builder.newInstance().type("any").properties(Map.of("bar", "foo")).build())
                .callbackAddresses(List.of(CallbackAddress.Builder.newInstance().uri("http://any").events(emptySet()).build()))
                .privateProperties(Map.of("fooPrivate", "bar"))
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

        var duty = DutyStep.Builder.newInstance().constraint(xoneConstraintStep).rule(mock()).build();
        var permission = PermissionStep.Builder.newInstance().constraint(orConstraintStep).dutyStep(duty).rule(mock()).build();
        var prohibition = ProhibitionStep.Builder.newInstance().constraint(andConstraintStep).rule(mock()).build();

        var validatorFunction = mock(PolicyValidatorRule.class);
        when(validatorFunction.name()).thenReturn("FunctionName");

        return PolicyEvaluationPlan.Builder.newInstance()
                .postValidator(new ValidatorStep(validatorFunction))
                .prohibition(prohibition)
                .permission(permission)
                .duty(duty)
                .preValidator(new ValidatorStep(validatorFunction))
                .build();
    }

    public static JsonObject policyDefinitionObject(String context) {
        return policyDefinitionObject(context, false);
    }

    public static JsonObject policyDefinitionObject(String context, boolean alwaysArray) {
        return createObjectBuilder()
                .add(CONTEXT, createContextBuilder(context).build())
                .add(TYPE, "PolicyDefinition")
                .add(ID, POLICY_ID)
                .add("policy", inForceDatePolicy("gteq", "contractAgreement+0s", "lteq", "contractAgreement+10s", alwaysArray))
                .add("privateProperties", createObjectBuilder()
                        .add("name", "policy-definition-name")
                        .add("id", POLICY_ID)
                        .add("description", "policy definition description")
                        .build())
                .build();
    }

    public static JsonObject policyDefinitionObject(String context, JsonObject permission) {
        return policyDefinitionObject(context, permission, false);
    }

    public static JsonObject policyDefinitionObject(String context, JsonObject permission, boolean alwaysArray) {
        return createObjectBuilder()
                .add(CONTEXT, createContextBuilder(context).build())
                .add(TYPE, "PolicyDefinition")
                .add(ID, POLICY_ID)
                .add("policy", policy(permission, alwaysArray))
                .build();
    }

    public static JsonObject secretObject(String context) {
        return createObjectBuilder()
                .add(CONTEXT, createContextBuilder(context).build())
                .add(TYPE, "Secret")
                .add(ID, "secret-id")
                .add("value", "superSecret")
                .build();
    }

    private static JsonArrayBuilder createCallbackAddress() {
        var builder = Json.createArrayBuilder();
        return builder.add(Json.createObjectBuilder()
                .add("@type", "CallbackAddress")
                .add("transactional", true)
                .add("uri", "http://test.local/")
                .add("events", Json.createArrayBuilder().add("event").build()));
    }

    public static JsonObjectBuilder policy(JsonObject permission, boolean alwaysArray) {
        return policy(permission, "Set", alwaysArray);
    }

    public static JsonObjectBuilder policy(JsonObject permission, String type, boolean alwaysArray) {
        var permissionValue = alwaysArray ? createArrayBuilder().add(permission).build() : permission;

        var builder = createObjectBuilder()
                .add(TYPE, type)
                .add("permission", permissionValue)
                .add("target", "assetId");

        if (!alwaysArray) {
            builder.add("prohibition", createArrayBuilder().build())
                    .add("obligation", createArrayBuilder().build());

        }
        return builder;
    }

    public static JsonObject querySpecObject(String context) {
        return querySpecObject(createObjectBuilder()
                .add(CONTEXT, createContextBuilder(context).build()));
    }

    public static JsonObject querySpecObject(JsonObjectBuilder builder) {
        var criterion = createObjectBuilder()
                .add(TYPE, "Criterion")
                .add("operandLeft", "foo")
                .add("operator", "=")
                .add("operandRight", "bar")
                .build();

        return builder
                .add(TYPE, "QuerySpec")
                .add("offset", 10)
                .add("limit", 20)
                .add("filterExpression", createArrayBuilder().add(criterion).build())
                .add("sortOrder", "DESC")
                .add("sortField", "fieldName")
                .build();

    }

    public static JsonObject embeddedQuerySpec() {
        return querySpecObject(createObjectBuilder());
    }

    public static JsonObject inForceDatePolicy(String operatorStart, Object startDate, String operatorEnd, Object endDate, boolean alwaysArray) {
        return policy(inForceDatePermission(operatorStart, startDate, operatorEnd, endDate, alwaysArray), alwaysArray).build();
    }

    public static JsonObject atomicConstraint(String leftOperand, String operator, Object rightOperand) {
        return createObjectBuilder()
                .add("leftOperand", leftOperand)
                .add("operator", operator)
                .add("rightOperand", rightOperand.toString())
                .build();
    }

    public static JsonObject inForceDatePermission(String operatorStart, Object startDate, String operatorEnd, Object endDate) {
        return inForceDatePermission(operatorStart, startDate, operatorEnd, endDate, false);
    }

    public static JsonObject inForceDatePermission(String operatorStart, Object startDate, String operatorEnd, Object endDate, boolean alwaysArray) {

        var and = createObjectBuilder()
                .add("and", createArrayBuilder()
                        .add(atomicConstraint("inForceDate", operatorStart, startDate))
                        .add(atomicConstraint("inForceDate", operatorEnd, endDate))
                        .build())
                .build();
        var constraint = alwaysArray ? createArrayBuilder().add(and).build() : and;
        return createObjectBuilder()
                .add("action", "use")
                .add("constraint", constraint)
                .build();
    }

    public static JsonObject policyEvaluationPlanRequest(String context) {
        return Json.createObjectBuilder()
                .add(CONTEXT, createContextBuilder(context).build())
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
