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

import org.eclipse.dataspaceconnector.junit.launcher.EdcRuntimeExtension;
import org.eclipse.dataspaceconnector.policy.model.Action;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.policy.model.PolicyType;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractOfferRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferType;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.io.File.separator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.dataspaceconnector.common.testfixtures.TestUtils.getFreePort;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.COMPLETED;
import static org.hamcrest.CoreMatchers.notNullValue;

class EndToEndTransferTest {

    private final Duration timeout = Duration.ofSeconds(30);
    private static final String API_KEY_CONTROL_AUTH = "password";

    private static final URI CONSUMER_CONTROL_PLANE = URI.create("http://localhost:" + getFreePort());
    private static final URI CONSUMER_CONTROL_PLANE_VALIDATION = URI.create("http://localhost:" + getFreePort() + "/validation");
    private static final URI CONSUMER_DATA_PLANE = URI.create("http://localhost:" + getFreePort());
    private static final URI CONSUMER_DATA_PLANE_PUBLIC = URI.create("http://localhost:" + getFreePort() + "/public");
    private static final URI CONSUMER_BACKEND_SERVICE = URI.create("http://localhost:" + getFreePort());
    private static final URI CONSUMER_IDS_API = URI.create("http://localhost:" + getFreePort());
    private static final URI PROVIDER_CONTROL_PLANE = URI.create("http://localhost:" + getFreePort());
    private static final URI PROVIDER_CONTROL_PLANE_VALIDATION = URI.create("http://localhost:" + getFreePort() + "/validation");
    private static final URI PROVIDER_DATA_PLANE = URI.create("http://localhost:" + getFreePort());
    private static final URI PROVIDER_DATA_PLANE_PUBLIC = URI.create("http://localhost:" + getFreePort() + "/public");
    private static final URI PROVIDER_BACKEND_SERVICE = URI.create("http://localhost:" + getFreePort());
    private static final URI PROVIDER_IDS_API = URI.create("http://localhost:" + getFreePort());

    @RegisterExtension
    static EdcRuntimeExtension consumerControlPlane = new EdcRuntimeExtension(
            ":system-tests:e2e-transfer-test:control-plane",
            "consumer-control-plane",
            new HashMap<>() {
                {
                    put("web.http.port", String.valueOf(CONSUMER_CONTROL_PLANE.getPort()));
                    put("web.http.path", "/api");
                    put("web.http.ids.port", String.valueOf(CONSUMER_IDS_API.getPort()));
                    put("web.http.ids.path", "/api/v1/ids");
                    put("web.http.validation.port", String.valueOf(CONSUMER_CONTROL_PLANE_VALIDATION.getPort()));
                    put("web.http.validation.path", "/validation");
                    put("edc.vault", resourceAbsolutePath("consumer-vault.properties"));
                    put("edc.keystore", resourceAbsolutePath("certs/cert.pfx"));
                    put("edc.keystore.password", "123456");
                    put("edc.api.control.auth.apikey.value", API_KEY_CONTROL_AUTH);
                    put("ids.webhook.address", CONSUMER_IDS_API.toString());
                    put("edc.receiver.http.endpoint", CONSUMER_BACKEND_SERVICE + "/api/service/pull");
                    put("edc.transfer.dataplane.token.signer.privatekey.alias", "1");
                    put("edc.public.key.alias", "public-key");
                    put("edc.transfer.dataplane.sync.endpoint", CONSUMER_DATA_PLANE_PUBLIC.toString());
                }
            }
    );

    @RegisterExtension
    static EdcRuntimeExtension consumerDataPlane = new EdcRuntimeExtension(
            ":system-tests:e2e-transfer-test:data-plane",
            "consumer-data-plane",
            new HashMap<>() {
                {
                    put("web.http.port", String.valueOf(CONSUMER_DATA_PLANE.getPort()));
                    put("web.http.path", "/api");
                    put("web.http.public.port", String.valueOf(CONSUMER_DATA_PLANE_PUBLIC.getPort()));
                    put("web.http.public.path", "/public");
                    put("web.http.control.port", String.valueOf(getFreePort()));
                    put("web.http.control.path", "/control");
                    put("edc.controlplane.validation-endpoint", CONSUMER_CONTROL_PLANE_VALIDATION + "/validation");
                }
            }
    );

    @RegisterExtension
    static EdcRuntimeExtension consumerBackendService = new EdcRuntimeExtension(
            ":system-tests:e2e-transfer-test:backend-service",
            "consumer-backend-service",
            new HashMap<>() {
                {
                    put("web.http.port", String.valueOf(CONSUMER_BACKEND_SERVICE.getPort()));
                }
            }
    );

    @RegisterExtension
    static EdcRuntimeExtension providerDataPlane = new EdcRuntimeExtension(
            ":system-tests:e2e-transfer-test:data-plane",
            "provider-data-plane",
            new HashMap<>() {
                {
                    put("web.http.port", String.valueOf(PROVIDER_DATA_PLANE.getPort()));
                    put("web.http.path", "/api");
                    put("web.http.public.port", String.valueOf(PROVIDER_DATA_PLANE_PUBLIC.getPort()));
                    put("web.http.public.path", "/public");
                    put("web.http.control.port", String.valueOf(getFreePort()));
                    put("web.http.control.path", "/control");
                    put("edc.controlplane.validation-endpoint", PROVIDER_CONTROL_PLANE_VALIDATION + "/validation");
                }
            }
    );

    @RegisterExtension
    static EdcRuntimeExtension providerControlPlane = new EdcRuntimeExtension(
            ":system-tests:e2e-transfer-test:control-plane",
            "provider-control-plane",
            new HashMap<>() {
                {
                    put("web.http.port", String.valueOf(PROVIDER_CONTROL_PLANE.getPort()));
                    put("web.http.path", "/api");
                    put("web.http.ids.port", String.valueOf(PROVIDER_IDS_API.getPort()));
                    put("web.http.ids.path", "/api/v1/ids");
                    put("web.http.validation.port", String.valueOf(PROVIDER_CONTROL_PLANE_VALIDATION.getPort()));
                    put("web.http.validation.path", "/validation");
                    put("edc.vault", resourceAbsolutePath("provider-vault.properties"));
                    put("edc.keystore", resourceAbsolutePath("certs/cert.pfx"));
                    put("edc.keystore.password", "123456");
                    put("edc.api.control.auth.apikey.value", API_KEY_CONTROL_AUTH);
                    put("ids.webhook.address", PROVIDER_IDS_API.toString());
                    put("edc.receiver.http.endpoint", PROVIDER_BACKEND_SERVICE + "/api/service/pull");
                    put("edc.transfer.dataplane.token.signer.privatekey.alias", "1");
                    put("edc.public.key.alias", "public-key");
                    put("edc.transfer.dataplane.sync.endpoint", PROVIDER_DATA_PLANE_PUBLIC.toString());
                }
            }
    );

    @RegisterExtension
    static EdcRuntimeExtension providerBackendService = new EdcRuntimeExtension(
            ":system-tests:e2e-transfer-test:backend-service",
            "provider-backend-service",
            new HashMap<>() {
                {
                    put("web.http.port", String.valueOf(PROVIDER_BACKEND_SERVICE.getPort()));
                }
            }
    );

    @Test
    void httpPullDataTransfer() {
        var assetId = createAsset(PROVIDER_CONTROL_PLANE);
        createContractDefinition(assetId, PROVIDER_CONTROL_PLANE);

        var negotiationId = negotiateContractFor(assetId, CONSUMER_CONTROL_PLANE, PROVIDER_IDS_API);

        var contractAgreementId = getContractAgreementId(negotiationId, CONSUMER_CONTROL_PLANE);

        assertThat(contractAgreementId).isNotEmpty();

        var transferProcessId = dataRequest(contractAgreementId, assetId, CONSUMER_CONTROL_PLANE, PROVIDER_IDS_API);

        await().atMost(timeout).untilAsserted(() -> {
            var transferProcess = getTransferProcess(transferProcessId, CONSUMER_CONTROL_PLANE);
            assertThat(transferProcess.getState()).isEqualTo(COMPLETED.code());
        });

        await().atMost(timeout).untilAsserted(() -> {
            given()
                    .baseUri(CONSUMER_BACKEND_SERVICE.toString())
                    .when()
                    .get("/api/service/providerData")
                    .then()
                    .statusCode(200)
                    .body(notNullValue());
        });
    }

    private TransferProcess getTransferProcess(String transferProcessId, URI instance) {
        return given()
                .baseUri(instance.toString())
                .contentType(JSON)
                .header("X-Api-Key", API_KEY_CONTROL_AUTH)
                .when()
                .get("/api/transfers/{id}", transferProcessId)
                .then()
                .statusCode(200)
                .extract().body().as(TransferProcess.class);
    }

    private String dataRequest(String contractAgreementId, String assetId, URI instance, URI provider) {
        var dataRequest = DataRequest.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .contractId(contractAgreementId)
                .connectorId("provider")
                .connectorAddress(provider + "/api/v1/ids/data")
                .protocol("ids-multipart")
                .assetId(assetId)
                .dataDestination(DataAddress.Builder.newInstance().type("HttpProxy").build())
                .managedResources(false)
                .transferType(TransferType.Builder.transferType()
                        .contentType("application/octet-stream")
                        .isFinite(true)
                        .build())
                .build();

        return given()
                .baseUri(instance.toString())
                .contentType(JSON)
                .header("X-Api-Key", API_KEY_CONTROL_AUTH)
                .body(dataRequest)
                .when()
                .post("/api/control/transfer")
                .then()
                .statusCode(200)
                .extract().body().asString();
    }

    private String getContractAgreementId(String negotiationId, URI instance) {
        var contractAgreementId = new AtomicReference<String>();

        await().atMost(timeout).untilAsserted(() -> {
            var result = given()
                    .baseUri(instance.toString())
                    .contentType(JSON)
                    .header("X-Api-Key", API_KEY_CONTROL_AUTH)
                    .when()
                    .get("/api/control/negotiation/{id}/state", negotiationId)
                    .then()
                    .statusCode(200)
                    .body("contractAgreementId", notNullValue())
                    .extract().body().jsonPath().getString("contractAgreementId");

            contractAgreementId.set(result);
        });

        return contractAgreementId.get();
    }

    private String negotiateContractFor(String assetId, URI instance, URI provider) {
        var request = ContractOfferRequest.Builder.newInstance()
                .connectorId("provider")
                .connectorAddress(provider + "/api/v1/ids/data")
                .protocol("ids-multipart")
                .contractOffer(ContractOffer.Builder.newInstance()
                        .id("1:1")
                        .policy(Policy.Builder.newInstance()
                                .id(UUID.randomUUID().toString())
                                .permission(Permission.Builder.newInstance()
                                        .target(assetId)
                                        .action(Action.Builder.newInstance().type("USE").build())
                                        .build())
                                .type(PolicyType.SET)
                                .build())
                        .provider(provider)
                        .consumer(instance)
                        .build())
                .build();

        return given()
                .baseUri(instance.toString())
                .contentType(JSON)
                .header("X-Api-Key", API_KEY_CONTROL_AUTH)
                .body(request)
                .when()
                .post("/api/control/negotiation")
                .then()
                .statusCode(200)
                .extract().body().asString();
    }

    private String createAsset(URI instance) {
        var asset = Map.of(
                "asset", Map.of(
                        "asset:prop:id", "asset-id",
                        "asset:prop:name", "asset name",
                        "asset:prop:contenttype", "text/plain",
                        "asset:prop:policy-id", "use-eu"
                ),
                "dataAddress", Map.of(
                        "endpoint", PROVIDER_BACKEND_SERVICE + "/api/service/data",
                        "type", "HttpData"
                )
        );

        return given()
                .baseUri(instance.toString())
                .contentType(JSON)
                .header("X-Api-Key", API_KEY_CONTROL_AUTH)
                .body(asset)
                .when()
                .post("/api/assets")
                .then()
                .statusCode(200)
                .extract().body().asString();
    }

    private void createContractDefinition(String assetId, URI instance) {
        var accessPolicy = Policy.Builder.newInstance()
                .permission(Permission.Builder.newInstance()
                        .target(assetId)
                        .action(Action.Builder.newInstance().type("USE").build())
                        .build())
                .type(PolicyType.SET)
                .build();
        var contractDefinition = ContractDefinition.Builder.newInstance()
                .id("1")
                .accessPolicy(accessPolicy)
                .contractPolicy(accessPolicy)
                .selectorExpression(AssetSelectorExpression.SELECT_ALL)
                .build();

        given()
                .baseUri(instance.toString())
                .contentType(JSON)
                .header("X-Api-Key", API_KEY_CONTROL_AUTH)
                .body(contractDefinition)
                .when()
                .post("/api/contractdefinitions")
                .then()
                .statusCode(204);
    }

    @NotNull
    private static String resourceAbsolutePath(String filename) {
        return System.getProperty("user.dir") + separator + "src" + separator + "test" + separator + "resources" + separator + filename;
    }
}
