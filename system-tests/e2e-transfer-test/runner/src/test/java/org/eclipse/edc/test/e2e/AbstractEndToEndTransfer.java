/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.test.e2e;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.contract.spi.ContractId;
import org.eclipse.edc.policy.model.Operator;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static java.time.Duration.ofDays;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.TERMINATED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_ACTION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_AND_CONSTRAINT_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_CONSTRAINT_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_CONSTRAINT_TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_LEFT_OPERAND_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_LOGICAL_CONSTRAINT_TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_OPERATOR_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_PERMISSION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_POLICY_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_RIGHT_OPERAND_ATTRIBUTE;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public abstract class AbstractEndToEndTransfer {

    protected static final Participant CONSUMER = new Participant("consumer", "urn:connector:consumer");
    protected static final Participant PROVIDER = new Participant("provider", "urn:connector:provider");
    private static final String CONTRACT_EXPIRY_EVALUATION_KEY = EDC_NAMESPACE + "inForceDate";
    protected final Duration timeout = Duration.ofSeconds(60);

    @Test
    void httpPullDataTransfer() {
        registerDataPlanes();
        var assetId = UUID.randomUUID().toString();
        createResourcesOnProvider(assetId, noConstraintPolicy(), UUID.randomUUID().toString(), httpDataAddressProperties());

        var dataset = CONSUMER.getDatasetForAsset(assetId, PROVIDER);
        var contractId = getContractId(dataset);
        var policy = dataset.getJsonArray(ODRL_POLICY_ATTRIBUTE).get(0).asJsonObject();

        var contractAgreementId = CONSUMER.negotiateContract(PROVIDER, contractId.toString(), contractId.assetIdPart(), policy);
        var transferProcessId = CONSUMER.initiateTransfer(contractAgreementId, assetId, PROVIDER, syncDataAddress());

        await().atMost(timeout).untilAsserted(() -> {
            var state = CONSUMER.getTransferProcessState(transferProcessId);
            assertThat(state).isEqualTo(STARTED.name());
        });

        // retrieve the data reference
        var edr = CONSUMER.getDataReference(transferProcessId);

        // pull the data without query parameter
        await().atMost(timeout).untilAsserted(() -> CONSUMER.pullData(edr, Map.of(), equalTo("some information")));

        // pull the data with additional query parameter
        var msg = UUID.randomUUID().toString();
        await().atMost(timeout).untilAsserted(() -> CONSUMER.pullData(edr, Map.of("message", msg), equalTo(msg)));
    }

    /**
     * This test is disabled because rejection messages are not processed correctly in IDS. The Policy that is attached to the contract definition
     * contains a validity period, that will be violated, so we expect the transfer request to be rejected with a 409 CONFLICT.
     * Once that case is handled properly by the DSP, we can re-enable the test and add a proper assertion
     */
    @Test
    void httpPull_withExpiredContract_fixedInForcePeriod() {
        registerDataPlanes();
        var assetId = UUID.randomUUID().toString();
        var now = Instant.now();
        // contract was valid from t-10d to t-5d, so "now" it is expired
        createResourcesOnProvider(assetId, inForcePolicy(Operator.GEQ, now.minus(ofDays(10)), Operator.LEQ, now.minus(ofDays(5))), UUID.randomUUID().toString(), httpDataAddressProperties());

        var dataset = CONSUMER.getDatasetForAsset(assetId, PROVIDER);
        var contractId = getContractId(dataset);
        var policy = dataset.getJsonArray(ODRL_POLICY_ATTRIBUTE).get(0).asJsonObject();

        var contractAgreementId = CONSUMER.negotiateContract(PROVIDER, contractId.toString(), contractId.assetIdPart(), policy);
        var transferProcessId = CONSUMER.initiateTransfer(contractAgreementId, assetId, PROVIDER, syncDataAddress());

        await().atMost(timeout).untilAsserted(() -> {
            var state = CONSUMER.getTransferProcessState(transferProcessId);
            assertThat(state).isEqualTo(TERMINATED.name());
        });
    }

    /**
     * This test is disabled because rejection messages are not processed correctly in IDS. The Policy that is attached to the contract definition
     * contains a validity period, that will be violated, so we expect the transfer request to be rejected with a 409 CONFLICT.
     * Once that case is handled properly by the DSP, we can re-enable the test and add a proper assertion
     */
    @Test
    void httpPull_withExpiredContract_durationInForcePeriod() {
        registerDataPlanes();
        var assetId = UUID.randomUUID().toString();
        var now = Instant.now();
        // contract was valid from t-10d to t-5d, so "now" it is expired
        createResourcesOnProvider(assetId, inForcePolicy(Operator.GEQ, now.minus(ofDays(10)), Operator.LEQ, "contractAgreement+1s"), UUID.randomUUID().toString(), httpDataAddressProperties());

        var dataset = CONSUMER.getDatasetForAsset(assetId, PROVIDER);
        var contractId = getContractId(dataset);
        var policy = dataset.getJsonArray(ODRL_POLICY_ATTRIBUTE).get(0).asJsonObject();

        var contractAgreementId = CONSUMER.negotiateContract(PROVIDER, contractId.toString(), contractId.assetIdPart(), policy);
        var transferProcessId = CONSUMER.initiateTransfer(contractAgreementId, assetId, PROVIDER, syncDataAddress());

        await().atMost(timeout).untilAsserted(() -> {
            var state = CONSUMER.getTransferProcessState(transferProcessId);
            assertThat(state).isEqualTo(TERMINATED.name());
        });
    }

    @Test
    void httpPullDataTransferProvisioner() {
        registerDataPlanes();
        var assetId = UUID.randomUUID().toString();
        createResourcesOnProvider(assetId, noConstraintPolicy(), UUID.randomUUID().toString(), Map.of(
                "name", "transfer-test",
                "baseUrl", PROVIDER.backendService() + "/api/provider/data",
                "type", "HttpProvision",
                "proxyQueryParams", "true"
        ));

        var dataset = CONSUMER.getDatasetForAsset(assetId, PROVIDER);
        var contractId = getContractId(dataset);
        var policy = dataset.getJsonArray(ODRL_POLICY_ATTRIBUTE).get(0).asJsonObject();

        var contractAgreementId = CONSUMER.negotiateContract(PROVIDER, contractId.toString(), contractId.assetIdPart(), policy);
        var transferProcessId = CONSUMER.initiateTransfer(contractAgreementId, assetId, PROVIDER, syncDataAddress());

        await().atMost(timeout).untilAsserted(() -> {
            var state = CONSUMER.getTransferProcessState(transferProcessId);
            assertThat(state).isEqualTo(STARTED.name());
        });

        var edr = CONSUMER.getDataReference(transferProcessId);
        await().atMost(timeout).untilAsserted(() -> CONSUMER.pullData(edr, Map.of(), equalTo("some information")));
    }

    @Test
    void httpPushDataTransfer() {
        registerDataPlanes();
        var assetId = UUID.randomUUID().toString();
        createResourcesOnProvider(assetId, noConstraintPolicy(), UUID.randomUUID().toString(), httpDataAddressProperties());

        var dataset = CONSUMER.getDatasetForAsset(assetId, PROVIDER);
        var contractId = getContractId(dataset);
        var policy = dataset.getJsonArray(ODRL_POLICY_ATTRIBUTE).get(0).asJsonObject();

        var contractAgreementId = CONSUMER.negotiateContract(PROVIDER, contractId.toString(), contractId.assetIdPart(), policy);

        var destination = httpDataAddress(CONSUMER.backendService() + "/api/consumer/store");
        var transferProcessId = CONSUMER.initiateTransfer(contractAgreementId, assetId, PROVIDER, destination);

        await().atMost(timeout).untilAsserted(() -> {
            var state = CONSUMER.getTransferProcessState(transferProcessId);
            assertThat(state).isEqualTo(STARTED.name());
        });

        await().atMost(timeout).untilAsserted(() -> {
            given()
                    .baseUri(CONSUMER.backendService().toString())
                    .when()
                    .get("/api/consumer/data")
                    .then()
                    .statusCode(anyOf(is(200), is(204)))
                    .body(is(notNullValue()));
        });
    }

    @Test
    @DisplayName("Provider pushes data to Consumer, Provider needs to authenticate the data request through an oauth2 server")
    void httpPushDataTransfer_oauth2Provisioning() {
        registerDataPlanes();
        var assetId = UUID.randomUUID().toString();
        createResourcesOnProvider(assetId, noConstraintPolicy(), UUID.randomUUID().toString(), httpDataAddressOauth2Properties());

        var dataset = CONSUMER.getDatasetForAsset(assetId, PROVIDER);
        var contractId = getContractId(dataset);
        var policy = dataset.getJsonArray(ODRL_POLICY_ATTRIBUTE).get(0).asJsonObject();

        var contractAgreementId = CONSUMER.negotiateContract(PROVIDER, contractId.toString(), contractId.assetIdPart(), policy);

        var destination = httpDataAddress(CONSUMER.backendService() + "/api/consumer/store");
        var transferProcessId = CONSUMER.initiateTransfer(contractAgreementId, assetId, PROVIDER, destination);

        await().atMost(timeout).untilAsserted(() -> {
            var state = CONSUMER.getTransferProcessState(transferProcessId);
            assertThat(state).isEqualTo(STARTED.name());
        });

        await().atMost(timeout).untilAsserted(() -> {
            given()
                    .baseUri(CONSUMER.backendService().toString())
                    .when()
                    .get("/api/consumer/data")
                    .then()
                    .statusCode(anyOf(is(200), is(204)))
                    .body(is(notNullValue()));
        });
    }

    private ContractId getContractId(JsonObject dataset) {
        var id = dataset.getJsonArray(ODRL_POLICY_ATTRIBUTE).get(0).asJsonObject().getString(ID);
        return ContractId.parse(id);
    }

    private JsonObject httpDataAddress(String baseUrl) {
        return Json.createObjectBuilder()
                .add(TYPE, EDC_NAMESPACE + "DataAddress")
                .add(EDC_NAMESPACE + "type", "HttpData")
                .add(EDC_NAMESPACE + "properties", Json.createObjectBuilder()
                        .add(EDC_NAMESPACE + "baseUrl", baseUrl)
                        .build())
                .build();
    }

    private JsonObject syncDataAddress() {
        return Json.createObjectBuilder()
                .add(TYPE, EDC_NAMESPACE + "DataAddress")
                .add(EDC_NAMESPACE + "type", "HttpProxy")
                .build();
    }

    @NotNull
    private Map<String, String> httpDataAddressOauth2Properties() {
        return Map.of(
                "name", "transfer-test",
                "baseUrl", PROVIDER.backendService() + "/api/provider/oauth2data",
                "type", "HttpData",
                "proxyQueryParams", "true",
                "oauth2:clientId", "clientId",
                "oauth2:clientSecretKey", "provision-oauth-secret",
                "oauth2:tokenUrl", PROVIDER.backendService() + "/api/oauth2/token"
        );
    }

    @NotNull
    private Map<String, String> httpDataAddressProperties() {
        return Map.of(
                "name", "transfer-test",
                "baseUrl", PROVIDER.backendService() + "/api/provider/data",
                "type", "HttpData",
                "proxyQueryParams", "true"
        );
    }

    private void registerDataPlanes() {
        PROVIDER.registerDataPlane();
        CONSUMER.registerDataPlane();
    }

    private void createResourcesOnProvider(String assetId, JsonObject contractPolicy, String definitionId, Map<String, String> dataAddressProperties) {
        PROVIDER.createAsset(assetId, dataAddressProperties);
        var accessPolicy = noConstraintPolicy();
        var accessPolicyId = PROVIDER.createPolicyDefinition(accessPolicy);
        var contractPolicyId = PROVIDER.createPolicyDefinition(contractPolicy);
        PROVIDER.createContractDefinition(assetId, definitionId, accessPolicyId, contractPolicyId);
    }

    private JsonObject noConstraintPolicy() {
        return Json.createObjectBuilder()
                .add(TYPE, "use")
                .build();
    }

    private JsonObject inForcePolicy(Operator operatorStart, Object startDate, Operator operatorEnd, Object endDate) {
        return Json.createObjectBuilder()
                .add(ODRL_PERMISSION_ATTRIBUTE, Json.createArrayBuilder()
                        .add(permission(operatorStart, startDate, operatorEnd, endDate)))
                .build();
    }

    private JsonObject permission(Operator operatorStart, Object startDate, Operator operatorEnd, Object endDate) {
        return Json.createObjectBuilder()
                .add(ODRL_ACTION_ATTRIBUTE, "USE")
                .add(ODRL_CONSTRAINT_ATTRIBUTE, Json.createObjectBuilder()
                        .add(TYPE, ODRL_LOGICAL_CONSTRAINT_TYPE)
                        .add(ODRL_AND_CONSTRAINT_ATTRIBUTE, Json.createArrayBuilder()
                                .add(atomicConstraint(CONTRACT_EXPIRY_EVALUATION_KEY, operatorStart, startDate))
                                .add(atomicConstraint(CONTRACT_EXPIRY_EVALUATION_KEY, operatorEnd, endDate))
                                .build())
                        .build())
                .build();
    }

    private JsonObject atomicConstraint(String leftOperand, Operator operator, Object rightOperand) {
        return Json.createObjectBuilder()
                .add(TYPE, ODRL_CONSTRAINT_TYPE)
                .add(ODRL_LEFT_OPERAND_ATTRIBUTE, leftOperand)
                .add(ODRL_OPERATOR_ATTRIBUTE, operator.toString())
                .add(ODRL_RIGHT_OPERAND_ATTRIBUTE, rightOperand.toString())
                .build();
    }
}
