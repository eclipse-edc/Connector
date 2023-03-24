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

import org.eclipse.edc.catalog.spi.Catalog;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.transfer.spi.types.TransferType;
import org.eclipse.edc.policy.model.PolicyRegistrationTypes;
import org.eclipse.edc.spi.asset.AssetSelectorExpression;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.edc.sql.testfixtures.PostgresqlLocalInstance;
import org.hamcrest.Matcher;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.io.File.separator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getFreePort;

public class Participant {
    private static final String IDS_PATH = "/api/v1/ids";


    private final Duration timeout = Duration.ofSeconds(30);

    private final URI controlPlane = URI.create("http://localhost:" + getFreePort());
    private final URI controlPlaneControl = URI.create("http://localhost:" + getFreePort() + "/control");
    private final URI controlPlaneManagement = URI.create("http://localhost:" + getFreePort() + "/api/management");
    private final URI dataPlane = URI.create("http://localhost:" + getFreePort());
    private final URI dataPlaneControl = URI.create("http://localhost:" + getFreePort() + "/control");
    private final URI dataPlanePublic = URI.create("http://localhost:" + getFreePort() + "/public");
    private final URI backendService = URI.create("http://localhost:" + getFreePort());
    private final URI idsEndpoint = URI.create("http://localhost:" + getFreePort());
    private final URI connectorId = URI.create("urn:connector:" + UUID.randomUUID());
    private final String name;
    private final TypeManager typeManager = new TypeManager();

    public Participant(String name) {
        this.name = name;
        PolicyRegistrationTypes.TYPES.forEach(typeManager::registerTypes);
    }

    public void createAsset(String assetId, Map<String, String> dataAddressProperties) {
        var asset = Map.of(
                "asset", Map.of(
                        "id", assetId,
                        "properties", Map.of(
                                "asset:prop:id", assetId,
                                "asset:prop:description", "description"
                        )
                ),
                "dataAddress", Map.of(
                        "properties", dataAddressProperties
                )
        );

        given()
                .baseUri(controlPlaneManagement.toString())
                .contentType(JSON)
                .body(asset)
                .when()
                .post("/assets")
                .then()
                .statusCode(200)
                .contentType(JSON);
    }

    public void createPolicy(PolicyDefinition policyDefinition) {
        given()
                .baseUri(controlPlaneManagement.toString())
                .contentType(JSON)
                .body(policyDefinition)
                .when()
                .post("/policydefinitions")
                .then()
                .statusCode(200)
                .contentType(JSON).contentType(JSON);
    }

    public void createContractDefinition(String assetId, String definitionId, String accessPolicyId, String contractPolicyId, long contractValidityDurationSeconds) {
        var contractDefinition = Map.of(
                "id", definitionId,
                "accessPolicyId", accessPolicyId,
                "validity", String.valueOf(contractValidityDurationSeconds),
                "contractPolicyId", contractPolicyId,
                "criteria", AssetSelectorExpression.Builder.newInstance().constraint("asset:prop:id", "=", assetId).build().getCriteria()
        );

        given()
                .baseUri(controlPlaneManagement.toString())
                .contentType(JSON)
                .body(contractDefinition)
                .when()
                .post("/contractdefinitions")
                .then()
                .statusCode(200)
                .contentType(JSON).contentType(JSON);
    }

    /**
     * Start contract negotiation, waits for agreement, check asset id and returns the agreement id
     */
    public String negotiateContract(Participant provider, ContractOffer contractOffer) {
        var request = Map.of(
                "connectorId", "provider",
                "consumerId", connectorId.toString(),
                "providerId", provider.connectorId.toString(),
                "connectorAddress", provider.idsEndpoint() + "/api/v1/ids/data",
                "protocol", "ids-multipart",
                "offer", Map.of(
                        "offerId", contractOffer.getId(),
                        "assetId", contractOffer.getAsset().getId(),
                        "policy", contractOffer.getPolicy()
                )
        );

        var negotiationId = given()
                .baseUri(controlPlaneManagement.toString())
                .contentType(JSON)
                .body(typeManager.writeValueAsString(request))
                .when()
                .post("/contractnegotiations")
                .then()
                .statusCode(200)
                .extract().body().jsonPath().getString("id");

        var contractAgreementId = getContractAgreementId(negotiationId);

        var assetId = getContractAgreementField(contractAgreementId, "assetId");
        assertThat(assetId).isEqualTo(contractOffer.getAsset().getId());

        return contractAgreementId;
    }

    public String getContractAgreementId(String negotiationId) {
        var contractAgreementId = new AtomicReference<String>();

        await().atMost(timeout).untilAsserted(() -> {
            var agreementId = getContractNegotiationField(negotiationId, "contractAgreementId");
            assertThat(agreementId).isNotNull().isInstanceOf(String.class);

            contractAgreementId.set(agreementId);
        });

        var id = contractAgreementId.get();
        assertThat(id).isNotEmpty();
        return id;
    }

    public String dataRequest(String id, String contractAgreementId, String assetId, Participant provider, DataAddress dataAddress) {
        var request = Map.of(
                "id", id,
                "contractId", contractAgreementId,
                "assetId", assetId,
                "connectorId", "provider",
                "connectorAddress", provider.idsEndpoint() + "/api/v1/ids/data",
                "protocol", "ids-multipart",
                "dataDestination", dataAddress,
                "managedResources", false,
                "transferType", TransferType.Builder.transferType()
                        .contentType("application/octet-stream")
                        .isFinite(true)
                        .build()
        );

        return given()
                .baseUri(controlPlaneManagement.toString())
                .contentType(JSON)
                .body(request)
                .when()
                .post("/transferprocess")
                .then()
                .statusCode(200)
                .extract().body().jsonPath().getString("id");
    }

    public String getTransferProcessState(String transferProcessId) {
        return given()
                .baseUri(controlPlaneManagement.toString())
                .contentType(JSON)
                .when()
                .get("/transferprocess/{id}/state", transferProcessId)
                .then()
                .statusCode(200)
                .extract().body().jsonPath().getString("state");
    }

    public EndpointDataReference getDataReference(String id) {
        var dataReference = new AtomicReference<EndpointDataReference>();

        await().atMost(timeout).untilAsserted(() -> {
            var result = given()
                    .baseUri(backendService.toString())
                    .when()
                    .get("/api/consumer/dataReference/{id}", id)
                    .then()
                    .statusCode(200)
                    .extract()
                    .body()
                    .as(EndpointDataReference.class);
            dataReference.set(result);
        });

        return dataReference.get();
    }

    public void pullData(EndpointDataReference edr, Map<String, String> queryParams, Matcher<String> bodyMatcher) {
        given()
                .baseUri(edr.getEndpoint())
                .header(edr.getAuthKey(), edr.getAuthCode())
                .queryParams(queryParams)
                .when()
                .get()
                .then()
                .statusCode(200)
                .body("message", bodyMatcher);
    }

    public URI backendService() {
        return backendService;
    }

    public void registerDataPlane() {
        var body = Map.of(
                "edctype", "dataspaceconnector:dataplaneinstance",
                "id", UUID.randomUUID().toString(),
                "url", dataPlaneControl + "/transfer",
                "allowedSourceTypes", List.of("HttpData", "HttpProvision"),
                "allowedDestTypes", List.of("HttpData", "HttpProvision", "HttpProxy"),
                "properties", Map.of("publicApiUrl", dataPlanePublic.toString())
        );

        given()
                .baseUri(controlPlaneManagement.toString())
                .contentType(JSON)
                .body(body)
                .when()
                .post("/instances")
                .then()
                .statusCode(204);
    }

    public Catalog getCatalog(Participant provider) {
        var catalogReference = new AtomicReference<Catalog>();

        await().atMost(timeout).untilAsserted(() -> {
            var response = given()
                    .baseUri(controlPlaneManagement.toString())
                    .contentType(JSON)
                    .when()
                    .queryParam("providerUrl", provider.idsEndpoint() + IDS_PATH + "/data")
                    .get("/catalog")
                    .then()
                    .statusCode(200)
                    .extract().body().asString();

            var catalog = typeManager.readValue(response, Catalog.class);

            assertThat(catalog.getContractOffers())
                    .hasSizeGreaterThan(0)
                    .allMatch(offer -> connectorId.equals(offer.getConsumer()))
                    .allMatch(offer -> provider.connectorId.equals(offer.getProvider()));

            catalogReference.set(catalog);
        });

        return catalogReference.get();
    }

    public URI idsEndpoint() {
        return idsEndpoint;
    }

    public Map<String, String> controlPlaneConfiguration() {
        return new HashMap<>() {
            {
                put("web.http.port", String.valueOf(controlPlane.getPort()));
                put("web.http.path", "/api");
                put("web.http.ids.port", String.valueOf(idsEndpoint.getPort()));
                put("web.http.ids.path", IDS_PATH);
                put("web.http.management.port", String.valueOf(controlPlaneManagement.getPort()));
                put("web.http.management.path", controlPlaneManagement.getPath());
                put("web.http.control.port", String.valueOf(controlPlaneControl.getPort()));
                put("web.http.control.path", controlPlaneControl.getPath());
                put("edc.ids.id", connectorId.toString());
                put("edc.vault", resourceAbsolutePath(name + "-vault.properties"));
                put("edc.keystore", resourceAbsolutePath("certs/cert.pfx"));
                put("edc.keystore.password", "123456");
                put("ids.webhook.address", idsEndpoint.toString());
                put("edc.receiver.http.endpoint", backendService + "/api/consumer/dataReference");
                put("edc.transfer.proxy.token.signer.privatekey.alias", "1");
                put("edc.transfer.proxy.token.verifier.publickey.alias", "public-key");
                put("edc.transfer.proxy.endpoint", dataPlanePublic.toString());
                put("edc.negotiation.consumer.send.retry.limit", "1");
                put("edc.negotiation.provider.send.retry.limit", "1");
                put("edc.negotiation.consumer.send.retry.base-delay.ms", "100");
                put("edc.negotiation.provider.send.retry.base-delay.ms", "100");

                put("provisioner.http.entries.default.provisioner.type", "provider");
                put("provisioner.http.entries.default.endpoint", backendService + "/api/provision");
                put("provisioner.http.entries.default.data.address.type", "HttpProvision");
            }
        };
    }

    public Map<String, String> controlPlanePostgresConfiguration() {
        var baseConfiguration = controlPlaneConfiguration();

        var postgresConfiguration = new HashMap<String, String>() {
            {
                put("edc.datasource.asset.name", "asset");
                put("edc.datasource.asset.url", jdbcUrl());
                put("edc.datasource.asset.user", PostgresqlLocalInstance.USER);
                put("edc.datasource.asset.password", PostgresqlLocalInstance.PASSWORD);
                put("edc.datasource.contractdefinition.name", "contractdefinition");
                put("edc.datasource.contractdefinition.url", jdbcUrl());
                put("edc.datasource.contractdefinition.user", PostgresqlLocalInstance.USER);
                put("edc.datasource.contractdefinition.password", PostgresqlLocalInstance.PASSWORD);
                put("edc.datasource.contractnegotiation.name", "contractnegotiation");
                put("edc.datasource.contractnegotiation.url", jdbcUrl());
                put("edc.datasource.contractnegotiation.user", PostgresqlLocalInstance.USER);
                put("edc.datasource.contractnegotiation.password", PostgresqlLocalInstance.PASSWORD);
                put("edc.datasource.policy.name", "policy");
                put("edc.datasource.policy.url", jdbcUrl());
                put("edc.datasource.policy.user", PostgresqlLocalInstance.USER);
                put("edc.datasource.policy.password", PostgresqlLocalInstance.PASSWORD);
                put("edc.datasource.transferprocess.name", "transferprocess");
                put("edc.datasource.transferprocess.url", jdbcUrl());
                put("edc.datasource.transferprocess.user", PostgresqlLocalInstance.USER);
                put("edc.datasource.transferprocess.password", PostgresqlLocalInstance.PASSWORD);
            }
        };
        baseConfiguration.putAll(postgresConfiguration);

        return baseConfiguration;
    }

    public Map<String, String> controlPlaneCosmosDbConfiguration(String uniqueTestName) {
        var baseConfiguration = controlPlaneConfiguration();
        var cosmosDbConfiguration = new HashMap<String, String>() {
            {
                put("edc.assetindex.cosmos.account-name", "test");
                put("edc.assetindex.cosmos.database-name", uniqueTestName);
                put("edc.assetindex.cosmos.container-name", name + "-assetindex");
                put("edc.contractdefinitionstore.cosmos.account-name", "test");
                put("edc.contractdefinitionstore.cosmos.database-name", uniqueTestName);
                put("edc.contractdefinitionstore.cosmos.container-name", name + "-contractdefinitionstore");
                put("edc.contractnegotiationstore.cosmos.account-name", "test");
                put("edc.contractnegotiationstore.cosmos.database-name", uniqueTestName);
                put("edc.contractnegotiationstore.cosmos.container-name", name + "-contractnegotiationstore");
                put("edc.node.directory.cosmos.account.name", "test");
                put("edc.node.directory.cosmos.database.name", uniqueTestName);
                put("edc.node.directory.cosmos.container.name", name + "-nodedirectory");
                put("edc.policystore.cosmos.account-name", "test");
                put("edc.policystore.cosmos.database-name", uniqueTestName);
                put("edc.policystore.cosmos.container-name", name + "-policystore");
                put("edc.transfer-process-store.cosmos.account.name", "test");
                put("edc.transfer-process-store.database.name", uniqueTestName);
                put("edc.transfer-process-store.cosmos.container-name", name + "-transfer-process-store");
            }
        };
        baseConfiguration.putAll(cosmosDbConfiguration);

        return baseConfiguration;
    }

    @NotNull
    public String jdbcUrl() {
        return PostgresqlLocalInstance.JDBC_URL_PREFIX + name;
    }

    public Map<String, String> dataPlaneConfiguration() {
        return new HashMap<>() {
            {
                put("web.http.port", String.valueOf(dataPlane.getPort()));
                put("web.http.path", "/api");
                put("web.http.public.port", String.valueOf(dataPlanePublic.getPort()));
                put("web.http.public.path", "/public");
                put("web.http.control.port", String.valueOf(dataPlaneControl.getPort()));
                put("web.http.control.path", dataPlaneControl.getPath());
                put("edc.vault", resourceAbsolutePath(name + "-vault.properties"));
                put("edc.keystore", resourceAbsolutePath("certs/cert.pfx"));
                put("edc.keystore.password", "123456");
                put("edc.dataplane.token.validation.endpoint", controlPlaneControl + "/token");
            }
        };
    }

    public String getName() {
        return name;
    }

    private String getContractAgreementField(String contractAgreementId, String fieldName) {
        return given()
                .baseUri(controlPlaneManagement.toString())
                .contentType(JSON)
                .when()
                .get("/contractagreements/{id}", contractAgreementId)
                .then()
                .statusCode(200)
                .extract().body().jsonPath()
                .getString(fieldName);
    }

    private String getContractNegotiationField(String negotiationId, String fieldName) {
        return given()
                .baseUri(controlPlaneManagement.toString())
                .contentType(JSON)
                .when()
                .get("/contractnegotiations/{id}", negotiationId)
                .then()
                .statusCode(200)
                .extract().body().jsonPath()
                .getString(fieldName);
    }

    @NotNull
    private String resourceAbsolutePath(String filename) {
        return System.getProperty("user.dir") + separator + "build" + separator + "resources" + separator + "test" + separator + filename;
    }
}
