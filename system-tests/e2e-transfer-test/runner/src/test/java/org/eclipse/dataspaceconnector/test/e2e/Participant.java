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

import org.eclipse.dataspaceconnector.policy.model.Action;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.policy.model.PolicyType;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.catalog.Catalog;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferType;
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
import static org.awaitility.Awaitility.await;
import static org.eclipse.dataspaceconnector.common.testfixtures.TestUtils.getFreePort;
import static org.hamcrest.CoreMatchers.notNullValue;

public class Participant {

    private static final String IDS_PATH = "/api/v1/ids";
    private final Duration timeout = Duration.ofSeconds(30);

    private final URI controlPlane = URI.create("http://localhost:" + getFreePort());
    private final URI controlPlaneValidation = URI.create("http://localhost:" + getFreePort() + "/validation");
    private final URI controlPlaneDataplane = URI.create("http://localhost:" + getFreePort() + "/dataplane");
    private final URI dataPlane = URI.create("http://localhost:" + getFreePort());
    private final URI dataPlaneControl = URI.create("http://localhost:" + getFreePort() + "/control");
    private final URI dataPlanePublic = URI.create("http://localhost:" + getFreePort() + "/public");
    private final URI backendService = URI.create("http://localhost:" + getFreePort());
    private final URI idsEndpoint = URI.create("http://localhost:" + getFreePort());

    public void createAsset(String assetId) {
        var asset = Map.of(
                "asset", Map.of(
                        "properties", Map.of(
                                "asset:prop:id", assetId,
                                "asset:prop:name", "asset name",
                                "asset:prop:contenttype", "text/plain",
                                "asset:prop:policy-id", "use-eu"
                        )
                ),
                "dataAddress", Map.of(
                        "properties", Map.of(
                                "name", "data",
                                "endpoint", backendService + "/api/service",
                                "type", "HttpData"
                        )
                )
        );

        given()
                .baseUri(controlPlane.toString())
                .contentType(JSON)
                .body(asset)
                .when()
                .post("/api/assets")
                .then()
                .statusCode(204);
    }

    public String createPolicy(String assetId) {
        var policy = Policy.Builder.newInstance()
                .permission(Permission.Builder.newInstance()
                        .target(assetId)
                        .action(Action.Builder.newInstance().type("USE").build())
                        .build())
                .type(PolicyType.SET)
                .build();

        given()
                .baseUri(controlPlane.toString())
                .contentType(JSON)
                .body(policy)
                .when()
                .post("/api/policies")
                .then()
                .statusCode(204);

        return policy.getUid();
    }

    public void createContractDefinition(String policyId) {
        var contractDefinition = Map.of(
                "id", "1",
                "accessPolicyId", policyId,
                "contractPolicyId", policyId,
                "criteria", AssetSelectorExpression.SELECT_ALL.getCriteria()
        );

        given()
                .baseUri(controlPlane.toString())
                .contentType(JSON)
                .body(contractDefinition)
                .when()
                .post("/api/contractdefinitions")
                .then()
                .statusCode(204);
    }

    public String negotiateContract(String assetId, Participant provider) {
        var policy = Policy.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .permission(Permission.Builder.newInstance()
                        .target(assetId)
                        .action(Action.Builder.newInstance().type("USE").build())
                        .build())
                .type(PolicyType.SET)
                .build();
        var request = Map.of(
                "connectorId", "provider",
                "connectorAddress", provider.idsEndpoint() + "/api/v1/ids/data",
                "protocol", "ids-multipart",
                "offer", Map.of(
                        "offerId", "1:1",
                        "assetId", assetId,
                        "policy", policy
                )
        );

        return given()
                .baseUri(controlPlane.toString())
                .contentType(JSON)
                .body(request)
                .when()
                .post("/api/contractnegotiations")
                .then()
                .statusCode(200)
                .extract().body().jsonPath().getString("id");
    }

    public String getContractAgreementId(String negotiationId) {
        var contractAgreementId = new AtomicReference<String>();

        await().atMost(timeout).untilAsserted(() -> {
            var result = given()
                    .baseUri(controlPlane.toString())
                    .contentType(JSON)
                    .when()
                    .get("/api/contractnegotiations/{id}", negotiationId)
                    .then()
                    .statusCode(200)
                    .body("contractAgreementId", notNullValue())
                    .extract().body().jsonPath().getString("contractAgreementId");

            contractAgreementId.set(result);
        });

        return contractAgreementId.get();
    }

    public String dataRequest(String contractAgreementId, String assetId, Participant provider, DataAddress dataAddress) {
        var request = Map.of(
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

    public URI backendService() {
        return backendService;
    }

    public void registerDataPlane() {
        var body = Map.of(
                "edctype", "dataspaceconnector:dataplaneinstance",
                "id", UUID.randomUUID().toString(),
                "url", dataPlaneControl + "/transfer",
                "allowedSourceTypes", List.of("HttpData"),
                "allowedDestTypes", List.of("HttpData")
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
        return given()
                .baseUri(controlPlane.toString())
                .contentType(JSON)
                .when()
                .queryParam("providerUrl", provider + IDS_PATH + "/data")
                .get("/api/catalog")
                .then()
                .statusCode(200)
                .extract().body().as(Catalog.class);
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
                put("web.http.validation.port", String.valueOf(controlPlaneValidation.getPort()));
                put("web.http.validation.path", controlPlaneValidation.getPath());
                put("edc.vault", resourceAbsolutePath("consumer-vault.properties"));
                put("edc.keystore", resourceAbsolutePath("certs/cert.pfx"));
                put("edc.keystore.password", "123456");
                put("ids.webhook.address", idsEndpoint.toString());
                put("edc.receiver.http.endpoint", backendService + "/api/service/pull");
                put("edc.transfer.dataplane.token.signer.privatekey.alias", "1");
                put("edc.public.key.alias", "public-key");
                put("edc.transfer.dataplane.sync.endpoint", dataPlanePublic.toString());
            }
        };
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
                put("edc.controlplane.validation-endpoint", controlPlaneValidation + "/validation");
            }
        };
    }

    @NotNull
    private String resourceAbsolutePath(String filename) {
        return System.getProperty("user.dir") + separator + "src" + separator + "test" + separator + "resources" + separator + filename;
    }
}
