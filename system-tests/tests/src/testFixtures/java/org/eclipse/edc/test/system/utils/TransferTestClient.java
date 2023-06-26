/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.test.system.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.specification.RequestSpecification;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.connector.contract.spi.ContractId;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static jakarta.json.Json.createObjectBuilder;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.FINALIZED;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_DATASET_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_POLICY_ATTRIBUTE;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.CoreConstants.EDC_PREFIX;
import static org.eclipse.edc.test.system.local.TransferRuntimeConfiguration.CONSUMER_PARTICIPANT_ID;
import static org.hamcrest.Matchers.is;

/**
 * Simple client for testing transfer scenario
 */
public class TransferTestClient {

    private final String consumerUrl;
    private final ObjectMapper objectMapper = JacksonJsonLd.createObjectMapper();
    private final JsonLd jsonLd = new TitaniumJsonLd(new ConsoleMonitor());

    public TransferTestClient(String consumerUrl) {
        this.consumerUrl = consumerUrl;
    }

    public JsonArray getCatalogDatasets(String providerUrl) {
        var datasetReference = new AtomicReference<JsonArray>();
        var requestBody = createObjectBuilder()
                .add(CONTEXT, createObjectBuilder().add(EDC_PREFIX, EDC_NAMESPACE))
                .add(TYPE, "CatalogRequest")
                .add("providerUrl", providerUrl)
                .add("protocol", "dataspace-protocol-http")
                .build();

        await().untilAsserted(() -> {
            var response = givenConsumerRequest()
                    .contentType(JSON)
                    .when()
                    .body(requestBody)
                    .post("/v2/catalog/request")
                    .then()
                    .statusCode(200)
                    .extract().body().asString();

            var responseBody = objectMapper.readValue(response, JsonObject.class);

            var catalog = jsonLd.expand(responseBody).orElseThrow(f -> new EdcException(f.getFailureDetail()));

            var datasets = catalog.getJsonArray(DCAT_DATASET_ATTRIBUTE);
            assertThat(datasets).hasSizeGreaterThan(0);

            datasetReference.set(datasets);
        });

        return datasetReference.get();
    }

    public JsonObject getDatasetForAsset(String assetId, String providerUrl) {
        var datasets = getCatalogDatasets(providerUrl);
        return datasets.stream()
                .map(JsonValue::asJsonObject)
                .filter(it -> assetId.equals(getContractId(it).assetIdPart()))
                .findFirst()
                .orElseThrow(() -> new EdcException(format("No dataset for asset %s in the catalog", assetId)));
    }

    public String negotiateContract(String providerId, String providerUrl, String offerId, String assetId, JsonObject policy) {
        var requestBody = createObjectBuilder()
                .add(CONTEXT, createObjectBuilder().add(EDC_PREFIX, EDC_NAMESPACE))
                .add(TYPE, "NegotiationInitiateRequestDto")
                .add("connectorId", providerId)
                .add("consumerId", CONSUMER_PARTICIPANT_ID)
                .add("providerId", providerId)
                .add("connectorAddress", providerUrl)
                .add("protocol", "dataspace-protocol-http")
                .add("offer", createObjectBuilder()
                        .add("offerId", offerId)
                        .add("assetId", assetId)
                        .add("policy", jsonLd.compact(policy).getContent())
                )
                .build();

        var negotiationId = given()
                .baseUri(consumerUrl)
                .contentType(JSON)
                .body(requestBody)
                .when()
                .post("/v2/contractnegotiations")
                .then()
                .statusCode(200)
                .extract().body().jsonPath().getString(ID);

        await().untilAsserted(() -> {
            var state = getContractNegotiationState(negotiationId);
            assertThat(state).isEqualTo(FINALIZED.name());
        });

        return getContractAgreementId(negotiationId);
    }

    public String getContractAgreementId(String negotiationId) {
        var contractAgreementId = new AtomicReference<String>();

        await().untilAsserted(() -> {
            var agreementId = getContractNegotiationField(negotiationId, "contractAgreementId");
            assertThat(agreementId).isNotNull().isInstanceOf(String.class);

            contractAgreementId.set(agreementId);
        });

        var id = contractAgreementId.get();
        assertThat(id).isNotEmpty();
        return id;
    }

    private String getContractNegotiationField(String negotiationId, String fieldName) {
        return given()
                .baseUri(consumerUrl)
                .contentType(JSON)
                .when()
                .get("/v2/contractnegotiations/{id}", negotiationId)
                .then()
                .statusCode(200)
                .extract().body().jsonPath()
                .getString(format("'edc:%s'", fieldName));
    }

    public String initiateTransfer(String contractId, String assetId, String providerUrl, JsonObject destination) {
        var requestBody = createObjectBuilder()
                .add(CONTEXT, createObjectBuilder().add(EDC_PREFIX, EDC_NAMESPACE))
                .add(TYPE, "TransferRequestDto")
                .add("dataDestination", destination)
                .add("protocol", "dataspace-protocol-http")
                .add("assetId", assetId)
                .add("contractId", contractId)
                .add("connectorAddress", providerUrl)
                .add("managedResources", true)
                .build();

        return given()
                .baseUri(consumerUrl)
                .contentType(JSON)
                .body(requestBody)
                .when()
                .post("/v2/transferprocesses")
                .then()
                .statusCode(200)
                .extract().body().jsonPath().getString(ID);
    }

    private RequestSpecification givenConsumerRequest() {
        return given()
                .baseUri(consumerUrl);
    }

    static ContractId getContractId(JsonObject dataset) {
        var id = dataset.getJsonArray(ODRL_POLICY_ATTRIBUTE).get(0).asJsonObject().getString(ID);
        return ContractId.parseId(id).getContent();
    }

    public Map<String, String> getTransferProcess(String transferProcessId) {
        return given()
                .baseUri(consumerUrl)
                .contentType(JSON)
                .when()
                .get("/v2/transferprocesses/{id}", transferProcessId)
                .then()
                .statusCode(200)
                .body("'edc:state'", is(STARTED.name()))
                .extract().jsonPath().get("'edc:dataDestination'");
    }

    public String getTransferProcessState(String transferProcessId) {
        return given()
                .baseUri(consumerUrl)
                .contentType(JSON)
                .when()
                .get("/v2/transferprocesses/{id}/state", transferProcessId)
                .then()
                .statusCode(200)
                .extract().body().jsonPath().getString("'edc:state'");
    }

    public String getContractNegotiationState(String id) {
        return given()
                .baseUri(consumerUrl)
                .contentType(JSON)
                .when()
                .get("/v2/contractnegotiations/{id}/state", id)
                .then()
                .statusCode(200)
                .extract().body().jsonPath().getString("'edc:state'");
    }
}
