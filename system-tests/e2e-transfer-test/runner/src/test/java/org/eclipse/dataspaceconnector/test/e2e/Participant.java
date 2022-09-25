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

package org.eclipse.dataspaceconnector.test.e2e;

import org.eclipse.dataspaceconnector.common.util.postgres.PostgresqlLocalInstance;
import org.eclipse.dataspaceconnector.policy.model.PolicyRegistrationTypes;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.policy.PolicyDefinition;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.catalog.Catalog;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferType;
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
import static org.eclipse.dataspaceconnector.junit.testfixtures.TestUtils.getFreePort;

public class Participant {

    private static final String IDS_PATH = "/api/v1/ids";
    private final Duration timeout = Duration.ofSeconds(30);

    private final URI controlPlane = URI.create("http://localhost:" + getFreePort());
    private final URI controlPlaneValidation = URI.create("http://localhost:" + getFreePort() + "/validation");
    private final URI controlPlaneDataplane = URI.create("http://localhost:" + getFreePort() + "/dataplane");
    private final URI controlPlaneProvisioner = URI.create("http://localhost:" + getFreePort() + "/provisioner");
    private final URI dataPlane = URI.create("http://localhost:" + getFreePort());
    private final URI dataPlaneControl = URI.create("http://localhost:" + getFreePort() + "/control");
    private final URI dataPlanePublic = URI.create("http://localhost:" + getFreePort() + "/public");
    private final URI backendService = URI.create("http://localhost:" + getFreePort());
    private final URI idsEndpoint = URI.create("http://localhost:" + getFreePort());
    private final String name;
    private final TypeManager typeManager = new TypeManager();

    public Participant(String name) {
        this.name = name;
        PolicyRegistrationTypes.TYPES.forEach(typeManager::registerTypes);
    }

    public void createAsset(String assetId, String addressType) {
        var asset = Map.of(
                "asset", Map.of(
                        "id", assetId,
                        "properties", Map.of(
                                "asset:prop:id", assetId,
                                "asset:prop:description", "description"
                        )
                ),
                "dataAddress", Map.of(
                        "properties", Map.of(
                                "name", "transfer-test",
                                "baseUrl", backendService + "/api/provider/data",
                                "type", addressType,
                                "proxyQueryParams", "true"
                        )
                )
        );

        given()
                .baseUri(controlPlane.toString())
                .contentType(JSON)
                .body(asset)
                .when()
                .post("/api/assets")
                .then();
    }

    public void createPolicy(PolicyDefinition policyDefinition) {
        given()
                .baseUri(controlPlane.toString())
                .contentType(JSON)
                .body(policyDefinition)
                .when()
                .post("/api/policydefinitions")
                .then()
                .statusCode(200);
    }

    public void createContractDefinition(String assetId, String definitionId, String accessPolicyId, String contractPolicyId) {
        var contractDefinition = Map.of(
                "id", definitionId,
                "accessPolicyId", accessPolicyId,
                "contractPolicyId", contractPolicyId,
                "criteria", AssetSelectorExpression.Builder.newInstance().constraint("asset:prop:id", "=", assetId).build().getCriteria()
        );

        given()
                .baseUri(controlPlane.toString())
                .contentType(JSON)
                .body(contractDefinition)
                .when()
                .post("/api/contractdefinitions")
                .then();
    }

    public String negotiateContract(Participant provider, ContractOffer contractOffer) {
        var request = Map.of(
                "connectorId", "provider",
                "connectorAddress", provider.idsEndpoint() + "/api/v1/ids/data",
                "protocol", "ids-multipart",
                "offer", Map.of(
                        "offerId", contractOffer.getId(),
                        "assetId", contractOffer.getAsset().getId(),
                        "policy", contractOffer.getPolicy()
                )
        );

        return given()
                .baseUri(controlPlane.toString())
                .contentType(JSON)
                .body(typeManager.writeValueAsString(request))
                .when()
                .post("/api/contractnegotiations")
                .then()
                .statusCode(200)
                .extract().body().jsonPath().getString("id");
    }

    public String getContractAgreementId(String negotiationId) {
        var contractAgreementId = new AtomicReference<String>();

        await().atMost(timeout).untilAsserted(() -> {
            var agreementId = getContractNegotiationField(negotiationId, "contractAgreementId");
            assertThat(agreementId).isNotNull().isInstanceOf(String.class);

            contractAgreementId.set(agreementId);
        });

        return contractAgreementId.get();
    }

    public String getContractNegotiationState(String negotiationId) {
        return getContractNegotiationField(negotiationId, "state");
    }

    private String getContractNegotiationField(String negotiationId, String fieldName) {
        return given()
                .baseUri(controlPlane.toString())
                .contentType(JSON)
                .when()
                .get("/api/contractnegotiations/{id}", negotiationId)
                .then()
                .statusCode(200)
                .extract().body().jsonPath()
                .getString(fieldName);
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
                .baseUri(controlPlane.toString())
                .contentType(JSON)
                .body(request)
                .when()
                .post("/api/transferprocess")
                .then()
                .statusCode(200)
                .extract().body().jsonPath().getString("id");
    }

    public String getTransferProcessState(String transferProcessId) {
        return given()
                .baseUri(controlPlane.toString())
                .contentType(JSON)
                .when()
                .get("/api/transferprocess/{id}/state", transferProcessId)
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
                .baseUri(controlPlaneDataplane.toString())
                .contentType(JSON)
                .body(body)
                .when()
                .post("/instances")
                .then()
                .statusCode(204);
    }

    public Catalog getCatalog(URI provider) {
        String response = given()
                .baseUri(controlPlane.toString())
                .contentType(JSON)
                .when()
                .queryParam("providerUrl", provider + IDS_PATH + "/data")
                .get("/api/catalog")
                .then()
                .statusCode(200)
                .extract().body().asString();

        return typeManager.readValue(response, Catalog.class);
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
                put("web.http.dataplane.port", String.valueOf(controlPlaneDataplane.getPort()));
                put("web.http.dataplane.path", controlPlaneDataplane.getPath());
                put("web.http.provisioner.port", String.valueOf(controlPlaneProvisioner.getPort()));
                put("web.http.provisioner.path", controlPlaneProvisioner.getPath());
                put("web.http.validation.port", String.valueOf(controlPlaneValidation.getPort()));
                put("web.http.validation.path", controlPlaneValidation.getPath());
                put("edc.vault", resourceAbsolutePath("consumer-vault.properties"));
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

    public Map<String, String> controlPlaneCosmosDbConfiguration() {
        var baseConfiguration = controlPlaneConfiguration();

        var cosmosDbConfiguration = new HashMap<String, String>() {
            {
                put("edc.assetindex.cosmos.account-name", "test");
                put("edc.assetindex.cosmos.database-name", "e2e-transfer-test");
                put("edc.assetindex.cosmos.container-name", "assetindex");
                put("edc.contractdefinitionstore.cosmos.account-name", "test");
                put("edc.contractdefinitionstore.cosmos.database-name", "e2e-transfer-test");
                put("edc.contractdefinitionstore.cosmos.container-name", "contractdefinitionstore");
                put("edc.contractnegotiationstore.cosmos.account-name", "test");
                put("edc.contractnegotiationstore.cosmos.database-name", "e2e-transfer-test");
                put("edc.contractnegotiationstore.cosmos.container-name", "contractnegotiationstore");
                put("edc.node.directory.cosmos.account.name", "test");
                put("edc.node.directory.cosmos.database.name", "e2e-transfer-test");
                put("edc.node.directory.cosmos.container.name", "nodedirectory");
                put("edc.policystore.cosmos.account-name", "test");
                put("edc.policystore.cosmos.database-name", "e2e-transfer-test");
                put("edc.policystore.cosmos.container-name", "policystore");
                put("edc.transfer-process-store.cosmos.account.name", "test");
                put("edc.transfer-process-store.database.name", "e2e-transfer-test");
                put("edc.transfer-process-store.cosmos.container-name", "transfer-process-store");
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
                put("edc.dataplane.token.validation.endpoint", controlPlaneValidation + "/token");
            }
        };
    }

    public String getName() {
        return name;
    }

    @NotNull
    private String resourceAbsolutePath(String filename) {
        return System.getProperty("user.dir") + separator + "build" + separator + "resources" + separator + "test" + separator + filename;
    }
}
