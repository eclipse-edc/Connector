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
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.connector.contract.spi.ContractId;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;

import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static jakarta.json.Json.createObjectBuilder;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.FINALIZED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_DATASET_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_POLICY_ATTRIBUTE;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.CoreConstants.EDC_PREFIX;

/**
 * Essentially a wrapper around the management API enabling to test interactions with other participants, eg. catalog, transfer...
 */
public class Participant {

    private static final String DSP_PROTOCOL = "dataspace-protocol-http";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    protected String id;
    protected String name;
    protected Endpoint managementEndpoint;
    protected Endpoint protocolEndpoint;
    protected JsonLd jsonLd;
    protected ObjectMapper objectMapper;

    protected Participant() {
    }

    public String getName() {
        return name;
    }

    /**
     * Create a new {@link org.eclipse.edc.spi.types.domain.asset.Asset}.
     *
     * @param assetId               asset id
     * @param properties            asset properties
     * @param dataAddressProperties data address properties
     * @return id of the created asset.
     */
    public String createAsset(String assetId, Map<String, Object> properties, Map<String, Object> dataAddressProperties) {
        var requestBody = createObjectBuilder()
                .add(CONTEXT, createObjectBuilder().add(EDC_PREFIX, EDC_NAMESPACE))
                .add(ID, assetId)
                .add("properties", createObjectBuilder(properties))
                .add("dataAddress", createObjectBuilder(dataAddressProperties))
                .build();

        return managementEndpoint.baseRequest()
                .contentType(JSON)
                .body(requestBody)
                .when()
                .post("/v3/assets")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .extract().jsonPath().getString(ID);
    }

    /**
     * Create a new {@link org.eclipse.edc.connector.policy.spi.PolicyDefinition}.
     *
     * @param policy Json-LD representation of the policy
     * @return id of the created policy.
     */
    public String createPolicyDefinition(JsonObject policy) {
        var requestBody = createObjectBuilder()
                .add(CONTEXT, createObjectBuilder().add(EDC_PREFIX, EDC_NAMESPACE))
                .add(TYPE, "PolicyDefinitionDto")
                .add("policy", policy)
                .build();

        return managementEndpoint.baseRequest()
                .contentType(JSON)
                .body(requestBody)
                .when()
                .post("/v2/policydefinitions")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .extract().jsonPath().getString(ID);
    }

    /**
     * Create a new {@link org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition}.
     *
     * @param assetId          asset id
     * @param definitionId     contract definition id
     * @param accessPolicyId   access policy id
     * @param contractPolicyId contract policy id
     * @return id of the created contract definition.
     */
    public String createContractDefinition(String assetId, String definitionId, String accessPolicyId, String contractPolicyId) {
        var requestBody = createObjectBuilder()
                .add(ID, definitionId)
                .add(TYPE, EDC_NAMESPACE + "ContractDefinition")
                .add(EDC_NAMESPACE + "accessPolicyId", accessPolicyId)
                .add(EDC_NAMESPACE + "contractPolicyId", contractPolicyId)
                .add(EDC_NAMESPACE + "assetsSelector", Json.createArrayBuilder()
                        .add(createObjectBuilder()
                                .add(TYPE, "CriterionDto")
                                .add(EDC_NAMESPACE + "operandLeft", EDC_NAMESPACE + "id")
                                .add(EDC_NAMESPACE + "operator", "=")
                                .add(EDC_NAMESPACE + "operandRight", assetId)
                                .build())
                        .build())
                .build();

        return managementEndpoint.baseRequest()
                .contentType(JSON)
                .body(requestBody)
                .when()
                .post("/v2/contractdefinitions")
                .then()
                .statusCode(200)
                .extract().jsonPath().getString(ID);
    }

    /**
     * Request provider catalog.
     *
     * @param provider data provider
     * @return list of {@link org.eclipse.edc.catalog.spi.Dataset}.
     */
    public JsonArray getCatalogDatasets(Participant provider) {
        var datasetReference = new AtomicReference<JsonArray>();
        var requestBody = createObjectBuilder()
                .add(CONTEXT, createObjectBuilder().add(EDC_PREFIX, EDC_NAMESPACE))
                .add(TYPE, "CatalogRequest")
                .add("providerUrl", provider.protocolEndpoint.url.toString())
                .add("protocol", DSP_PROTOCOL)
                .build();

        await().atMost(TIMEOUT).untilAsserted(() -> {
            var response = managementEndpoint.baseRequest()
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

    /**
     * Get first {@link org.eclipse.edc.catalog.spi.Dataset} from provider matching the given asset id.
     *
     * @param provider data provider
     * @param assetId  asset id
     * @return dataset.
     */
    public JsonObject getDatasetForAsset(Participant provider, String assetId) {
        var datasets = getCatalogDatasets(provider);
        return datasets.stream()
                .map(JsonValue::asJsonObject)
                .filter(it -> assetId.equals(extractContractDefinitionId(it).assetIdPart()))
                .findFirst()
                .orElseThrow(() -> new EdcException(format("No dataset for asset %s in the catalog", assetId)));
    }

    /**
     * Initiate negotiation with a provider.
     *
     * @param provider data provider
     * @param offerId  contract definition id
     * @param assetId  asset id
     * @param policy   policy
     * @return id of the contract agreement.
     */
    public String negotiateContract(Participant provider, String offerId, String assetId, JsonObject policy) {
        var requestBody = createObjectBuilder()
                .add(CONTEXT, createObjectBuilder().add(EDC_PREFIX, EDC_NAMESPACE))
                .add(TYPE, "NegotiationInitiateRequestDto")
                .add("connectorId", provider.id)
                .add("consumerId", id)
                .add("providerId", provider.id)
                .add("connectorAddress", provider.protocolEndpoint.url.toString())
                .add("protocol", DSP_PROTOCOL)
                .add("offer", createObjectBuilder()
                        .add("offerId", offerId)
                        .add("assetId", assetId)
                        .add("policy", jsonLd.compact(policy).getContent())
                )
                .build();

        var negotiationId = managementEndpoint.baseRequest()
                .contentType(JSON)
                .body(requestBody)
                .when()
                .post("/v2/contractnegotiations")
                .then()
                .statusCode(200)
                .extract().body().jsonPath().getString(ID);

        await().atMost(TIMEOUT).untilAsserted(() -> {
            var state = getContractNegotiationState(negotiationId);
            assertThat(state).isEqualTo(FINALIZED.name());
        });

        return getContractAgreementId(negotiationId);
    }

    /**
     * Initiate data transfer.
     *
     * @param provider            data provider
     * @param contractAgreementId contract agreement id
     * @param assetId             asset id
     * @param privateProperties   private properties
     * @param destination         data destination address
     * @return id of the transfer process.
     */
    public String initiateTransfer(Participant provider, String contractAgreementId, String assetId, JsonObject privateProperties, JsonObject destination) {
        var requestBody = createObjectBuilder()
                .add(CONTEXT, createObjectBuilder().add(EDC_PREFIX, EDC_NAMESPACE))
                .add(TYPE, "TransferRequestDto")
                .add("dataDestination", destination)
                .add("protocol", DSP_PROTOCOL)
                .add("assetId", assetId)
                .add("contractId", contractAgreementId)
                .add("connectorId", provider.id)
                .add("connectorAddress", provider.protocolEndpoint.url.toString())
                .add("privateProperties", privateProperties)
                .build();

        return managementEndpoint.baseRequest()
                .contentType(JSON)
                .body(requestBody)
                .when()
                .post("/v2/transferprocesses")
                .then()
                .log().ifError()
                .statusCode(200)
                .extract().body().jsonPath().getString(ID);
    }

    /**
     * Request a provider asset:
     * - retrieves the contract definition associated with the asset,
     * - handles the contract negotiation,
     * - initiate the data transfer.
     *
     * @param provider          data provider
     * @param assetId           asset id
     * @param privateProperties private properties of the data request
     * @param destination       data destination
     * @return transfer process id.
     */
    public String requestAsset(Participant provider, String assetId, JsonObject privateProperties, JsonObject destination) {
        var dataset = getDatasetForAsset(provider, assetId);
        var policy = dataset.getJsonArray(ODRL_POLICY_ATTRIBUTE).get(0).asJsonObject();
        var contractDefinitionId = ContractId.parseId(policy.getString(ID))
                .orElseThrow(failure -> new RuntimeException(failure.getFailureDetail()));
        var contractAgreementId = negotiateContract(provider, contractDefinitionId.toString(), assetId, policy);
        var transferProcessId = initiateTransfer(provider, contractAgreementId, assetId, privateProperties, destination);
        assertThat(transferProcessId).isNotNull();
        return transferProcessId;
    }

    /**
     * Get current state of a transfer process.
     *
     * @param id transfer process id
     * @return state of the transfer process.
     */
    public String getTransferProcessState(String id) {
        return managementEndpoint.baseRequest()
                .contentType(JSON)
                .when()
                .get("/v2/transferprocesses/{id}/state", id)
                .then()
                .statusCode(200)
                .extract().body().jsonPath().getString("'edc:state'");
    }

    private ContractId extractContractDefinitionId(JsonObject dataset) {
        var contractId = dataset.getJsonArray(ODRL_POLICY_ATTRIBUTE).get(0).asJsonObject().getString(ID);
        return ContractId.parseId(contractId).orElseThrow(f -> new RuntimeException(f.getFailureDetail()));
    }

    private String getContractNegotiationState(String id) {
        return managementEndpoint.baseRequest()
                .contentType(JSON)
                .when()
                .get("/v2/contractnegotiations/{id}/state", id)
                .then()
                .statusCode(200)
                .extract().body().jsonPath().getString("'edc:state'");
    }


    private String getContractAgreementId(String negotiationId) {
        var contractAgreementIdAtomic = new AtomicReference<String>();

        await().atMost(TIMEOUT).untilAsserted(() -> {
            var agreementId = getContractNegotiationField(negotiationId, "contractAgreementId");
            assertThat(agreementId).isNotNull().isInstanceOf(String.class);

            contractAgreementIdAtomic.set(agreementId);
        });

        var contractAgreementId = contractAgreementIdAtomic.get();
        assertThat(id).isNotEmpty();
        return contractAgreementId;
    }

    private String getContractNegotiationField(String negotiationId, String fieldName) {
        return managementEndpoint.baseRequest()
                .contentType(JSON)
                .when()
                .get("/v2/contractnegotiations/{id}", negotiationId)
                .then()
                .statusCode(200)
                .extract().body().jsonPath()
                .getString(format("'edc:%s'", fieldName));
    }

    /**
     * Represent an endpoint exposed by a {@link Participant}.
     */
    public static class Endpoint {
        private final URI url;
        private final Map<String, String> headers;

        public Endpoint(URI url) {
            this.url = url;
            this.headers = new HashMap<>();
        }

        public Endpoint(URI url, Map<String, String> headers) {
            this.url = url;
            this.headers = headers;
        }

        public RequestSpecification baseRequest() {
            return given().baseUri(url.toString()).headers(headers);
        }

        public URI getUrl() {
            return url;
        }
    }

    public static class Builder<P extends Participant, B extends Participant.Builder<P, B>> {
        protected final P participant;

        protected Builder(P participant) {
            this.participant = participant;
        }

        public static <B extends Builder<Participant, B>> Builder<Participant, B> newInstance() {
            return new Builder<>(new Participant());
        }

        public B id(String id) {
            participant.id = id;
            return self();
        }

        public B name(String name) {
            participant.name = name;
            return self();
        }

        public B managementEndpoint(Endpoint managementEndpoint) {
            participant.managementEndpoint = managementEndpoint;
            return self();
        }

        public B protocolEndpoint(Endpoint protocolEndpoint) {
            participant.protocolEndpoint = protocolEndpoint;
            return self();
        }

        public B jsonLd(JsonLd jsonLd) {
            participant.jsonLd = jsonLd;
            return self();
        }

        public B objectMapper(ObjectMapper objectMapper) {
            participant.objectMapper = objectMapper;
            return self();
        }

        public Participant build() {
            Objects.requireNonNull(participant.id, "id");
            Objects.requireNonNull(participant.name, "name");
            Objects.requireNonNull(participant.managementEndpoint, "managementEndpoint");
            Objects.requireNonNull(participant.protocolEndpoint, "protocolEndpoint");
            if (participant.jsonLd == null) {
                participant.jsonLd = new TitaniumJsonLd(new ConsoleMonitor());
            }
            if (participant.objectMapper == null) {
                participant.objectMapper = JacksonJsonLd.createObjectMapper();
            }
            return participant;
        }

        @SuppressWarnings("unchecked")
        private B self() {
            return (B) this;
        }
    }
}
