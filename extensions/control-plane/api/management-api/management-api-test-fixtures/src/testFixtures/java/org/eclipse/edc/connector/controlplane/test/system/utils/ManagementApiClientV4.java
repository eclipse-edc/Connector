/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
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

package org.eclipse.edc.connector.controlplane.test.system.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.specification.RequestSpecification;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.connector.controlplane.catalog.spi.Dataset;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.junit.extensions.ComponentRuntimeContext;
import org.eclipse.edc.junit.utils.LazySupplier;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.query.Criterion;

import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static jakarta.json.Json.createObjectBuilder;
import static jakarta.json.stream.JsonCollectors.toJsonArray;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.FINALIZED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * Testing Client for the Management API v4.
 */
public class ManagementApiClientV4 {

    protected String participantId;
    protected LazySupplier<URI> controlPlaneManagement;
    protected LazySupplier<URI> controlPlaneProtocol;
    protected String protocolVersionPath = "/2025-1";
    protected UnaryOperator<RequestSpecification> enrichManagementRequest = r -> r;
    protected ObjectMapper objectMapper;
    protected Duration timeout = Duration.ofSeconds(30);
    protected String protocol = "dataspace-protocol-http:2025-1";

    protected ManagementApiClientV4() {
        super();
    }

    public static ManagementApiClientV4 forContext(ComponentRuntimeContext context) {
        var participantId = context.getConfig().getString("edc.participant.id");
        return ManagementApiClientV4.Builder.newInstance().participantId(participantId)
                .controlPlaneManagement(context.getEndpoint("management"))
                .controlPlaneProtocol(context.getEndpoint("protocol"))
                .build();
    }

    public Duration getTimeout() {
        return timeout;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        setProtocol(protocol, "");
    }

    public void setProtocol(String protocol, String path) {
        this.protocol = protocol;
        this.protocolVersionPath = path;
    }

    public String getProtocolVersionPath() {
        return protocolVersionPath;
    }

    public String getParticipantId() {
        return participantId;
    }

    /**
     * Get the protocol URL of the participant which is the protocol base path + protocol version path (empty by default).
     *
     * @return protocol URL
     */
    public String getProtocolUrl() {
        return controlPlaneProtocol.get() + protocolVersionPath;
    }

    /**
     * Create a new {@link org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset}.
     *
     * @param assetId               asset id
     * @param properties            asset properties
     * @param dataAddressProperties data address properties
     * @return id of the created asset.
     */
    public String createAsset(String assetId, Map<String, Object> properties, Map<String, Object> dataAddressProperties) {
        var requestBody = createObjectBuilder()
                .add(CONTEXT, jsonLdContext())
                .add(ID, assetId)
                .add(TYPE, "Asset")
                .add("properties", createObjectBuilder(properties))
                .add("dataAddress", createObjectBuilder(dataAddressProperties))
                .build();

        return baseManagementRequest()
                .contentType(JSON)
                .body(requestBody)
                .when()
                .post("/v4beta/assets")
                .then()
                .log().ifError()
                .statusCode(200)
                .contentType(JSON)
                .extract().jsonPath().getString(ID);
    }

    /**
     * Create a new {@link PolicyDefinition}.
     *
     * @param policy Json-LD representation of the policy
     * @return id of the created policy.
     */
    public String createPolicyDefinition(JsonObject policy) {
        var requestBody = createObjectBuilder()
                .add(CONTEXT, jsonLdContext())
                .add(TYPE, "PolicyDefinition")
                .add("policy", policy)
                .build();

        return baseManagementRequest()
                .contentType(JSON)
                .body(requestBody)
                .when()
                .post("/v4beta/policydefinitions")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .contentType(JSON)
                .extract().jsonPath().getString(ID);
    }

    /**
     * Create a new {@link ContractDefinition}.
     *
     * @param assetId          asset id
     * @param definitionId     contract definition id
     * @param accessPolicyId   access policy id
     * @param contractPolicyId contract policy id
     * @return id of the created contract definition.
     */
    public String createContractDefinition(String assetId, String definitionId, String accessPolicyId, String contractPolicyId) {
        var requestBody = createObjectBuilder()
                .add(CONTEXT, jsonLdContext())
                .add(ID, definitionId)
                .add(TYPE, "ContractDefinition")
                .add("accessPolicyId", accessPolicyId)
                .add("contractPolicyId", contractPolicyId)
                .add("assetsSelector", Json.createArrayBuilder()
                        .add(createObjectBuilder()
                                .add(TYPE, "Criterion")
                                .add("operandLeft", EDC_NAMESPACE + "id")
                                .add("operator", "=")
                                .add("operandRight", assetId)
                                .build())
                        .build())
                .build();

        return baseManagementRequest()
                .contentType(JSON)
                .body(requestBody)
                .when()
                .post("/v4beta/contractdefinitions")
                .then()
                .statusCode(200)
                .extract().jsonPath().getString(ID);
    }

    /**
     * Get first {@link Dataset} from provider matching the given asset id.
     *
     * @param provider data provider
     * @param assetId  asset id
     * @return dataset.
     */
    public JsonObject getDatasetForAsset(CounterParty provider, String assetId) {
        var requestBody = createObjectBuilder()
                .add(CONTEXT, jsonLdContext())
                .add(TYPE, "DatasetRequest")
                .add(ID, assetId)
                .add("counterPartyId", provider.participantId())
                .add("counterPartyAddress", provider.protocolUrl())
                .add("protocol", protocol)
                .build();

        var response = baseManagementRequest()
                .contentType(JSON)
                .body(requestBody)
                .when()
                .post("/v4beta/catalog/dataset/request")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .log().ifValidationFails()
                .extract();

        try {
            var responseBody = response.body().asString();
            return objectMapper.readValue(responseBody, JsonObject.class);
        } catch (JsonProcessingException e) {
            throw new EdcException("Cannot deserialize dataset", e);
        }
    }

    /**
     * Initiate negotiation with a provider for an asset.
     * - Fetches the dataset for the ID
     * - Extracts the first policy
     * - Starts the contract negotiation
     *
     * @param provider data provider
     * @param assetId  asset id
     * @return id of the contract negotiation.
     */
    public String initContractNegotiation(CounterParty provider, String assetId) {
        return initContractNegotiation(provider, getOfferForAsset(provider, assetId));
    }

    /**
     * Initiate negotiation with a provider given an input policy.
     *
     * @param provider data provider
     * @param policy   policy
     * @return id of the contract negotiation.
     */
    public String initContractNegotiation(CounterParty provider, JsonObject policy) {
        var requestBody = createObjectBuilder()
                .add(CONTEXT, jsonLdContext())
                .add(TYPE, "ContractRequest")
                .add("counterPartyAddress", provider.protocolUrl())
                .add("protocol", protocol)
                .add("policy", policy)
                .build();

        return baseManagementRequest()
                .contentType(JSON)
                .body(requestBody)
                .when()
                .post("/v3/contractnegotiations")
                .then()
                .log().ifError()
                .statusCode(200)
                .extract().body().jsonPath().getString(ID);
    }

    /**
     * Get current state of a contract negotiation.
     *
     * @param id contract negotiation id
     * @return state of a contract negotiation.
     */
    public String getContractNegotiationState(String id) {
        return getContractNegotiationField(id, "state");
    }

    public RequestSpecification baseManagementRequest() {
        var request = given().baseUri(controlPlaneManagement.get().toString());
        return enrichManagementRequest.apply(request);
    }

    protected String getContractNegotiationField(String negotiationId, String fieldName) {
        return baseManagementRequest()
                .contentType(JSON)
                .when()
                .get("/v4beta/contractnegotiations/{id}", negotiationId)
                .then()
                .statusCode(200)
                .extract().body().jsonPath()
                .getString(fieldName);
    }

    private JsonObject getOfferForAsset(CounterParty provider, String assetId) {
        var dataset = getDatasetForAsset(provider, assetId);
        var policy = dataset.getJsonArray("hasPolicy").get(0).asJsonObject();
        return createObjectBuilder(policy)
                .add("assigner", provider.participantId())
                .add("target", dataset.get(ID))
                .build();
    }

    /**
     * Initiate negotiation with a provider.
     *
     * @param provider data provider
     * @param assetId  asset id
     * @return id of the contract agreement.
     */
    public String negotiateContract(CounterParty provider, String assetId) {

        var negotiationId = initContractNegotiation(provider, assetId);

        await().atMost(timeout).untilAsserted(() -> {
            var state = getContractNegotiationState(negotiationId);
            assertThat(state).isEqualTo(FINALIZED.name());
        });

        return getContractAgreementId(negotiationId);
    }

    /**
     * Initiate negotiation with a provider.
     *
     * @param provider data provider
     * @param policy   policy
     * @return id of the contract agreement.
     */
    public String negotiateContract(CounterParty provider, JsonObject policy) {

        var negotiationId = initContractNegotiation(provider, policy);

        await().atMost(timeout).untilAsserted(() -> {
            var state = getContractNegotiationState(negotiationId);
            assertThat(state).isEqualTo(FINALIZED.name());
        });

        return getContractAgreementId(negotiationId);
    }

    private String getContractAgreementId(String negotiationId) {
        var contractAgreementIdAtomic = new AtomicReference<String>();

        await().atMost(timeout).untilAsserted(() -> {
            var agreementId = getContractNegotiationField(negotiationId, "contractAgreementId");
            assertThat(agreementId).isNotNull().isInstanceOf(String.class);

            contractAgreementIdAtomic.set(agreementId);
        });

        var contractAgreementId = contractAgreementIdAtomic.get();
        assertThat(participantId).isNotEmpty();
        return contractAgreementId;
    }

    public JsonObject getContractAgreement(String contractAgreementId) {
        var response = baseManagementRequest()
                .contentType(JSON)
                .when()
                .get("/v4beta/contractagreements/{id}", contractAgreementId)
                .then()
                .statusCode(200)
                .contentType(JSON)
                .log().ifValidationFails()
                .extract();

        try {
            var responseBody = response.body().asString();
            return objectMapper.readValue(responseBody, JsonObject.class);
        } catch (JsonProcessingException e) {
            throw new EdcException("Cannot deserialize dataset", e);
        }
    }

    public JsonArray queryContractAgreements(Criterion... criteria) {
        return queryContractAgreements(toQueryObject(criteria));
    }

    public JsonArray queryContractAgreements(JsonObject querySpec) {
        var response = baseManagementRequest()
                .contentType(JSON)
                .body(querySpec)
                .when()
                .post("/v4beta/contractagreements/request")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .log().ifValidationFails()
                .extract();

        try {
            var responseBody = response.body().asString();
            return objectMapper.readValue(responseBody, JsonArray.class);
        } catch (JsonProcessingException e) {
            throw new EdcException("Cannot deserialize dataset", e);
        }
    }

    public CounterParty asCounterParty() {
        return new CounterParty(participantId, getProtocolUrl());
    }

    private JsonObject toQueryObject(Criterion... criteria) {
        var criteriaJson = Arrays.stream(criteria)
                .map(it -> {
                            JsonValue operandRight;
                            if (it.getOperandRight() instanceof Collection<?> collection) {
                                operandRight = Json.createArrayBuilder(collection).build();
                            } else {
                                operandRight = Json.createValue(it.getOperandRight().toString());
                            }
                            return createObjectBuilder()
                                    .add(TYPE, "Criterion")
                                    .add("operandLeft", it.getOperandLeft().toString())
                                    .add("operator", it.getOperator())
                                    .add("operandRight", operandRight)
                                    .build();
                        }
                ).collect(toJsonArray());

        return createObjectBuilder()
                .add(CONTEXT, jsonLdContext())
                .add(TYPE, "QuerySpec")
                .add("filterExpression", criteriaJson)
                .build();
    }

    private JsonArray jsonLdContext() {
        return Json.createArrayBuilder()
                .add(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2)
                .build();
    }

    public static class Builder {
        protected final ManagementApiClientV4 participant;

        protected Builder(ManagementApiClientV4 participant) {
            this.participant = participant;
        }

        public static Builder newInstance() {
            return new Builder(new ManagementApiClientV4());
        }

        public Builder participantId(String id) {
            participant.participantId = id;
            return this;
        }

        public Builder protocol(String protocol) {
            participant.protocol = protocol;
            return this;
        }

        public Builder timeout(Duration timeout) {
            participant.timeout = timeout;
            return this;
        }

        public Builder objectMapper(ObjectMapper objectMapper) {
            participant.objectMapper = objectMapper;
            return this;
        }

        public Builder controlPlaneManagement(LazySupplier<URI> controlPlaneManagement) {
            participant.controlPlaneManagement = controlPlaneManagement;
            return this;
        }

        public Builder controlPlaneProtocol(LazySupplier<URI> controlPlaneProtocol) {
            participant.controlPlaneProtocol = controlPlaneProtocol;
            return this;
        }

        public ManagementApiClientV4 build() {
            Objects.requireNonNull(participant.participantId, "participantId");
            Objects.requireNonNull(participant.controlPlaneManagement, "controlPlaneManagement");
            Objects.requireNonNull(participant.controlPlaneProtocol, "controlPlaneProtocol");

            if (participant.objectMapper == null) {
                participant.objectMapper = JacksonJsonLd.createObjectMapper();
            }

            return participant;
        }

    }


}
