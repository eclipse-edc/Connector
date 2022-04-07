/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.transfer.provision.http.webhook;

import io.restassured.specification.RequestSpecification;
import org.eclipse.dataspaceconnector.api.auth.AuthenticationService;
import org.eclipse.dataspaceconnector.junit.launcher.EdcExtension;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DeprovisionedResource;
import org.eclipse.dataspaceconnector.transfer.provision.http.HttpWebhookExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.eclipse.dataspaceconnector.common.testfixtures.TestUtils.getFreePort;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;

@ExtendWith(EdcExtension.class)
class HttpProvisionerWebhookApiControllerIntegrationTest {

    private static final String PROVISIONER_BASE_PATH = "/api/v1/provisioner";
    private final int port = getFreePort();
    private final String authKey = "123456";

    public static Stream<Arguments> invalidRequestParams() {
        return Stream.of(
                Arguments.of(null, DataAddress.Builder.newInstance().type("foo").build(), "resourcename", "resourcedef"),
                Arguments.of("assetid", null, "resourcename", "resourcedef"),
                Arguments.of("assetid", DataAddress.Builder.newInstance().type("foo").build(), null, "resourcedef"),
                Arguments.of("assetid", DataAddress.Builder.newInstance().type("foo").build(), "resourcename", null)
        );
    }

    @BeforeEach
    void setUp(EdcExtension extension) {
        extension.registerSystemExtension(ServiceExtension.class, new DummyAuthenticationExtension());
        extension.registerSystemExtension(ServiceExtension.class, new HttpWebhookExtension());

        extension.setConfiguration(Map.of(
                "web.http.provisioner.port", String.valueOf(port),
                "web.http.provisioner.path", PROVISIONER_BASE_PATH,
                "edc.api.auth.key", authKey
        ));
    }

    @ParameterizedTest
    @MethodSource("invalidRequestParams")
    void callProvisionWebhook_invalidBody(String assetId, DataAddress cda, String resName, String resDefId) {
        var tpId = "tpId";
        var rq = ProvisionerWebhookRequest.Builder.newInstance()
                .assetId(assetId)
                .contentDataAddress(cda)
                .resourceName(resName)
                .resourceDefinitionId(resDefId)
                .build();

        baseRequest()
                .body(rq)
                .contentType("application/json")
                .post("/callback/{processId}/provision", Map.of("processId", tpId))
                .then()
                .statusCode(400)
                .body(containsString(""));
    }

    @Test
    void callProvisionWebhook() {
        var rq = ProvisionerWebhookRequest.Builder.newInstance()
                .assetId("test-asset")
                .contentDataAddress(DataAddress.Builder.newInstance().type("foo").build())
                .resourceName("resource-name")
                .resourceDefinitionId("resource-definition")
                .build();

        baseRequest()
                .body(rq)
                .contentType("application/json")
                .post("/callback/{processId}/provision", "tp-id")
                .then()
                .statusCode(allOf(greaterThanOrEqualTo(200), lessThan(300)))
                .body(anything());
    }

    @Test
    void callDeprovisionWebhook_invalidBody() {

        baseRequest()
                .contentType("application/json")
                .post("/callback/{processId}/deprovision", "tp-id")
                .then()
                .statusCode(equalTo(400))
                .body(anything());
    }

    @Test
    void callDeprovisionWebhook() {
        var rq = DeprovisionedResource.Builder.newInstance()
                .provisionedResourceId("resource-id")
                .errorMessage("some-error")
                .build();

        baseRequest()
                .body(rq)
                .contentType("application/json")
                .post("/callback/{processId}/deprovision", "tp-id")
                .then()
                .statusCode(allOf(greaterThanOrEqualTo(200), lessThan(300)))
                .body(anything());
    }

    private RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + port)
                .basePath(PROVISIONER_BASE_PATH)
                .header("x-api-key", authKey)
                .when();
    }

    @Provides(AuthenticationService.class)
    private static class DummyAuthenticationExtension implements ServiceExtension {
        @Override
        public void initialize(ServiceExtensionContext context) {
            context.registerService(AuthenticationService.class, h -> true);
        }
    }
}