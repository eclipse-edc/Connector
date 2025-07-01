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
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import org.eclipse.edc.connector.controlplane.api.management.contractnegotiation.model.NegotiationState;
import org.eclipse.edc.connector.controlplane.api.management.policy.model.PolicyEvaluationPlanRequest;
import org.eclipse.edc.connector.controlplane.api.management.policy.model.PolicyValidationResult;
import org.eclipse.edc.connector.controlplane.api.management.transferprocess.model.SuspendTransfer;
import org.eclipse.edc.connector.controlplane.api.management.transferprocess.model.TerminateTransfer;
import org.eclipse.edc.connector.controlplane.api.management.transferprocess.model.TransferState;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.contract.spi.types.command.TerminateNegotiationCommand;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequest;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferRequest;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.policy.model.AndConstraint;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.secret.Secret;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.REQUESTED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.MANAGEMENT_API_CONTEXT;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.MANAGEMENT_API_SCOPE;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.assetObject;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.contractDefinitionObject;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.contractRequestObject;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.createContractAgreement;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.createContractNegotiation;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.createEdrEntry;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.createPolicyEvaluationPlan;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.createTransferProcess;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.dataAddressObject;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.inForceDatePermission;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.policyDefinitionObject;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.policyEvaluationPlanRequest;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.querySpecObject;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.secretObject;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.suspendTransferObject;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.terminateNegotiationObject;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.terminateTransferObject;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.transferRequestObject;
import static org.eclipse.edc.util.io.Ports.getFreePort;

@EndToEndTest
public class SerdeEndToEndTest {

    @RegisterExtension
    private static final RuntimeExtension RUNTIME = new SerdeRuntime();

    @Test
    void ser_ContractAgreement() {
        var agreement = createContractAgreement("test-id");
        var compactResult = serialize(agreement);

        assertThat(compactResult).isNotNull();
        assertThat(compactResult.getString(ID)).isEqualTo(agreement.getId());
        assertThat(compactResult.getString(TYPE)).isEqualTo("ContractAgreement");
        assertThat(compactResult.getString("providerId")).isEqualTo(agreement.getProviderId());
        assertThat(compactResult.getString("consumerId")).isEqualTo(agreement.getConsumerId());
        assertThat(compactResult.getString("assetId")).isEqualTo(agreement.getAssetId());
        assertThat(compactResult.getJsonObject("policy")).isNotNull()
                .satisfies(policy -> {
                    assertThat(policy.get(TYPE)).isEqualTo(Json.createValue("Agreement"));
                });
        assertThat(compactResult.getJsonNumber("contractSigningDate")).isNotNull();

    }

    @Test
    void ser_ContractNegotiation() {
        var negotiation = createContractNegotiation();
        var compactResult = serialize(negotiation);

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

    }

    @Test
    void ser_TransferProcess() {
        var transferProcess = createTransferProcess();
        var compactResult = serialize(transferProcess);

        assertThat(compactResult).isNotNull();
        assertThat(compactResult.getString(ID)).isEqualTo(transferProcess.getId());
        assertThat(compactResult.getString(TYPE)).isEqualTo("TransferProcess");
        assertThat(compactResult.getString("correlationId")).isEqualTo(transferProcess.getCorrelationId());
        assertThat(compactResult.getString("state")).isEqualTo(transferProcess.stateAsString());
        assertThat(compactResult.getJsonNumber("stateTimestamp").longValue()).isEqualTo(transferProcess.getStateTimestamp());
        assertThat(compactResult.getString("assetId")).isEqualTo(transferProcess.getAssetId());
        assertThat(compactResult.getString("contractId")).isEqualTo(transferProcess.getContractId());
        assertThat(compactResult.getString("type")).isEqualTo(transferProcess.getType().toString());
        assertThat(compactResult.getJsonObject("dataDestination")).isNotNull();
        assertThat(compactResult.getJsonArray("callbackAddresses")).hasSize(transferProcess.getCallbackAddresses().size());
        assertThat(compactResult.getString("errorDetail")).isEqualTo(transferProcess.getErrorDetail());
    }

    @Test
    void ser_EndpointDataReferenceEntry() {
        var transferProcess = createEdrEntry();
        var compactResult = serialize(transferProcess);

        assertThat(compactResult).isNotNull();
        assertThat(compactResult.getString(ID)).isEqualTo(transferProcess.getId());
        assertThat(compactResult.getString(TYPE)).isEqualTo("EndpointDataReferenceEntry");
        assertThat(compactResult.getString("transferProcessId")).isEqualTo(transferProcess.getTransferProcessId());
        assertThat(compactResult.getString("contractNegotiationId")).isEqualTo(transferProcess.getContractNegotiationId());
        assertThat(compactResult.getString("assetId")).isEqualTo(transferProcess.getAssetId());
        assertThat(compactResult.getString("providerId")).isEqualTo(transferProcess.getProviderId());
        assertThat(compactResult.getString("agreementId")).isEqualTo(transferProcess.getAgreementId());
    }

    @Test
    void ser_NegotiationState() {
        var state = new NegotiationState(TransferProcessStates.REQUESTED.name());
        var compactResult = serialize(state);

        assertThat(compactResult).isNotNull();
        assertThat(compactResult.getString(TYPE)).isEqualTo("NegotiationState");
        assertThat(compactResult.getString("state")).isEqualTo(TransferProcessStates.REQUESTED.name());

    }

    @Test
    void ser_TransferState() {
        var state = new TransferState(REQUESTED.name());
        var compactResult = serialize(state);

        assertThat(compactResult).isNotNull();
        assertThat(compactResult.getString(TYPE)).isEqualTo("TransferState");
        assertThat(compactResult.getString("state")).isEqualTo(REQUESTED.name());

    }

    @Test
    void ser_PolicyValidationResult() {
        var result = new PolicyValidationResult(false, List.of("error1", "error2"));
        var compactResult = serialize(result);

        assertThat(compactResult).isNotNull();
        assertThat(compactResult.getString(TYPE)).isEqualTo("PolicyValidationResult");
        assertThat(compactResult.getBoolean("isValid")).isEqualTo(false);
        assertThat(compactResult.getJsonArray("errors")).contains(Json.createValue("error1"), Json.createValue("error2"));

    }

    @Test
    void ser_PolicyEvaluationPlan() {
        var plan = createPolicyEvaluationPlan();
        var compactResult = serialize(plan);

        assertThat(compactResult).isNotNull();
        assertThat(compactResult.getString(TYPE)).isEqualTo("PolicyEvaluationPlan");
        assertThat(compactResult.getJsonArray("preValidators")).hasSize(1);
        assertThat(compactResult.getJsonArray("postValidators")).hasSize(1);


        BiFunction<String, String, Consumer<JsonValue>> ruleAsser = (ruleType, multiplicityType) -> (ruleValue) -> {
            var rule = ruleValue.asJsonObject();
            assertThat(rule.getString(TYPE)).isEqualTo(ruleType);
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
    void de_ContractRequest() {
        var inputObject = contractRequestObject();
        var request = deserialize(inputObject, ContractRequest.class);

        assertThat(request).isNotNull();
        assertThat(request.getProviderId()).isEqualTo(inputObject.getJsonObject("policy").getString("assigner"));
        assertThat(request.getCallbackAddresses()).isNotEmpty();
        assertThat(request.getProtocol()).isEqualTo("test-protocol");
        assertThat(request.getCounterPartyAddress()).isEqualTo("test-address");
        assertThat(request.getContractOffer().getPolicy()).isNotNull();

    }

    @Test
    void de_TransferRequest() {
        var inputObject = transferRequestObject();
        var transferRequest = deserialize(inputObject, TransferRequest.class);

        assertThat(transferRequest).isNotNull();
        assertThat(transferRequest.getId()).isEqualTo(inputObject.getString(ID));
        assertThat(transferRequest.getCounterPartyAddress()).isEqualTo(inputObject.getString("counterPartyAddress"));
        assertThat(transferRequest.getContractId()).isEqualTo(inputObject.getString("contractId"));
        assertThat(transferRequest.getDataDestination()).extracting(DataAddress::getType).isEqualTo(inputObject.getJsonObject("dataDestination").getString("type"));
        assertThat(transferRequest.getPrivateProperties()).containsEntry(EDC_NAMESPACE + "fooPrivate", "bar");
        assertThat(transferRequest.getProtocol()).isEqualTo(inputObject.getString("protocol"));
        assertThat(transferRequest.getCallbackAddresses()).hasSize(inputObject.getJsonArray("callbackAddresses").size());
        assertThat(transferRequest.getTransferType()).isEqualTo(inputObject.getString("transferType"));
    }

    @Test
    void de_TransferRequest_withoutDataAddressType() {
        var dataDestination = createObjectBuilder()
                .add("type", "type").build();

        var inputObject = transferRequestObject(dataDestination);
        var transferRequest = deserialize(inputObject, TransferRequest.class);

        assertThat(transferRequest).isNotNull();
        assertThat(transferRequest.getId()).isEqualTo(inputObject.getString(ID));
        assertThat(transferRequest.getCounterPartyAddress()).isEqualTo(inputObject.getString("counterPartyAddress"));
        assertThat(transferRequest.getContractId()).isEqualTo(inputObject.getString("contractId"));
        assertThat(transferRequest.getDataDestination()).extracting(DataAddress::getType).isEqualTo(inputObject.getJsonObject("dataDestination").getString("type"));
        assertThat(transferRequest.getPrivateProperties()).containsEntry(EDC_NAMESPACE + "fooPrivate", "bar");
        assertThat(transferRequest.getProtocol()).isEqualTo(inputObject.getString("protocol"));
        assertThat(transferRequest.getCallbackAddresses()).hasSize(inputObject.getJsonArray("callbackAddresses").size());
        assertThat(transferRequest.getTransferType()).isEqualTo(inputObject.getString("transferType"));

    }


    @Test
    void de_TerminateNegotiation() {
        var inputObject = terminateNegotiationObject();
        var terminateNegotiation = deserialize(inputObject, TerminateNegotiationCommand.class);

        assertThat(terminateNegotiation).isNotNull();
        assertThat(terminateNegotiation.getReason()).isEqualTo(inputObject.getString("reason"));
    }

    @Test
    void de_TerminateTransfer() {
        var inputObject = terminateTransferObject();
        var terminateTransfer = deserialize(inputObject, TerminateTransfer.class);

        assertThat(terminateTransfer).isNotNull();
        assertThat(terminateTransfer.reason()).isEqualTo(inputObject.getString("reason"));

    }

    @Test
    void de_SuspendTransfer() {
        var inputObject = suspendTransferObject();
        var terminateTransfer = deserialize(inputObject, SuspendTransfer.class);

        assertThat(terminateTransfer).isNotNull();
        assertThat(terminateTransfer.reason()).isEqualTo(inputObject.getString("reason"));
        assertThat(terminateTransfer.reason()).isEqualTo(inputObject.getString("reason"));

    }

    @Test
    void de_PolicyDefinition_withInForceDate() {

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

        var inputObject = policyDefinitionObject(inForceDatePermission("gteq", "contractAgreement+0s", "lteq", "contractAgreement+10s"));

        var terminateTransfer = deserialize(inputObject, PolicyDefinition.class);

        assertThat(terminateTransfer).isNotNull();
        assertThat(terminateTransfer.getPolicy().getPermissions().get(0).getConstraints())
                .usingRecursiveFieldByFieldElementComparator().containsOnly(andConstraint);

    }

    @Test
    void de_PolicyEvaluationPlanRequest() {
        var inputObject = policyEvaluationPlanRequest();
        var terminateTransfer = deserialize(inputObject, PolicyEvaluationPlanRequest.class);

        assertThat(terminateTransfer).isNotNull();
        assertThat(terminateTransfer.policyScope()).isEqualTo(inputObject.getString("policyScope"));

    }

    /**
     * Tests for entities that supports transformation from/to JsonObject
     */
    @ParameterizedTest(name = "{1}")
    @ArgumentsSource(JsonInputProvider.class)
    void serde(JsonObject inputObject, Class<?> klass, Function<JsonObject, JsonObject> mapper) {
        var typeTransformerRegistry = RUNTIME.getService(TypeTransformerRegistry.class);
        var jsonLd = RUNTIME.getService(JsonLd.class);
        var registry = typeTransformerRegistry.forContext(MANAGEMENT_API_CONTEXT);

        // Expand the input
        var expanded = jsonLd.expand(inputObject).orElseThrow(f -> new AssertionError(f.getFailureDetail()));

        // transform the expanded into the input klass type
        var result = registry.transform(expanded, klass).orElseThrow(failure -> new RuntimeException());
        // transform the klass type instance into JsonObject
        var object = registry.transform(result, JsonObject.class).orElseThrow(failure -> new RuntimeException());

        // Compact the result
        var compactResult = jsonLd.compact(object, MANAGEMENT_API_SCOPE);

        // checks that the compacted == inputObject
        assertThat(compactResult).isSucceeded().satisfies(compacted -> {
            var mapped = Optional.ofNullable(mapper).map(m -> m.apply(compacted)).orElse(compacted);
            assertThat(mapped).isEqualTo(inputObject);
        });
    }

    private JsonObject serialize(Object object) {
        var typeTransformerRegistry = RUNTIME.getService(TypeTransformerRegistry.class);
        var registry = typeTransformerRegistry.forContext(MANAGEMENT_API_CONTEXT);
        var jsonLd = RUNTIME.getService(JsonLd.class);

        var result = registry.transform(object, JsonObject.class).orElseThrow(failure -> new RuntimeException());
        return jsonLd.compact(result, MANAGEMENT_API_SCOPE).orElseThrow(failure -> new RuntimeException(failure.getFailureDetail()));
    }

    private <T> T deserialize(JsonObject inputObject, Class<T> klass) {
        var typeTransformerRegistry = RUNTIME.getService(TypeTransformerRegistry.class);
        var registry = typeTransformerRegistry.forContext(MANAGEMENT_API_CONTEXT);
        var jsonLd = RUNTIME.getService(JsonLd.class);

        var expanded = jsonLd.expand(inputObject).orElseThrow(f -> new AssertionError(f.getFailureDetail()));


        // checks that the type is correctly expanded to the EDC namespace
        assertThat(expanded.getJsonArray(TYPE)).first().satisfies(t -> {
            assertThat(((JsonString) t).getString()).startsWith(EDC_NAMESPACE);
        });

        return registry.transform(expanded, klass).orElseThrow(failure -> new RuntimeException());
    }

    private static class JsonInputProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {

            Function<JsonObject, JsonObject> mapper = this::policyMapper;

            return Stream.of(
                    Arguments.of(assetObject(), Asset.class, null),
                    Arguments.of(contractDefinitionObject(), ContractDefinition.class, null),
                    Arguments.of(secretObject(), Secret.class, null),
                    Arguments.of(querySpecObject(), QuerySpec.class, null),
                    Arguments.of(policyDefinitionObject(), PolicyDefinition.class, mapper),
                    Arguments.of(dataAddressObject(), DataAddress.class, null)
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
    }


    static class SerdeRuntime extends ManagementEndToEndExtension {

        protected SerdeRuntime() {
            super(context());
        }

        private static ManagementEndToEndTestContext context() {
            var managementPort = getFreePort();
            var protocolPort = getFreePort();

            var runtime = new EmbeddedRuntime("control-plane", ":system-tests:management-api:management-api-test-runtime")
                    .configurationProvider(() -> ConfigFactory.fromMap(new HashMap<>() {
                        {
                            put("web.http.path", "/");
                            put("web.http.port", String.valueOf(getFreePort()));
                            put("web.http.protocol.path", "/protocol");
                            put("web.http.protocol.port", String.valueOf(protocolPort));
                            put("web.http.control.port", String.valueOf(getFreePort()));
                            put("edc.dsp.callback.address", "http://localhost:" + protocolPort + "/protocol");
                            put("web.http.management.path", "/management");
                            put("web.http.management.port", String.valueOf(managementPort));
                            put("edc.management.context.enabled", "true");
                        }
                    }));

            return new ManagementEndToEndTestContext(runtime);
        }

    }

}
