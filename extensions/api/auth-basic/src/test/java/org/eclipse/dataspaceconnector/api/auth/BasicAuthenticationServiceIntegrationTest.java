/*
 * Copyright (c) 2022 ZF Friedrichshafen AG
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 *    ZF Friedrichshafen AG - Initial API and Implementation
 */

package org.eclipse.dataspaceconnector.api.auth;

import io.restassured.http.Header;
import io.restassured.specification.RequestSpecification;
import org.eclipse.dataspaceconnector.junit.launcher.EdcExtension;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.eclipse.dataspaceconnector.common.testfixtures.TestUtils.getFreePort;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(EdcExtension.class)
public class BasicAuthenticationServiceIntegrationTest {

    private static final int port = getFreePort();
    private final Vault vault = mock(Vault.class);
    private final DummyApiExtension dummyApiExtension = new DummyApiExtension();

    @BeforeEach
    void setUp(EdcExtension extension) {
        extension.registerServiceMock(Vault.class, vault);
        extension.registerSystemExtension(ServiceExtension.class, dummyApiExtension);
        extension.setConfiguration(Map.of(
                "web.http.data.port", String.valueOf(port),
                "web.http.data.path", "/api/v1/data",
                "edc.api.auth.basic.vault-keys.hello", "api-basic-auth-hello"
        ));
    }

    @Test
    void noAuthHeader() {
        baseRequest()
                .header(new Header("NoAuth", "user aGVsbG86bXlQYXNzd29yZAp="))
                .get("/dummy")
                .then()
                .statusCode(403);
    }

    @Test
    void validateIncorrectAuthHeaderCredentials() {

        baseRequest()
                .header(new Header("Authorization", "user aGVsbG86bXlQYXNzd29yZAp="))
                .get("/dummy")
                .then()
                .statusCode(403);
    }

    @Test
    void validateCorrectAuthCredentials() {
        when(vault.resolveSecret("api-basic-auth-hello")).thenReturn("myPassword");

        baseRequest()
                .header(new Header("Authorization", "Basic aGVsbG86bXlQYXNzd29yZA=="))
                .get("/dummy")
                .then()
                .statusCode(200);
    }

    private RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + port)
                .basePath("/api/v1/data")
                .when();
    }
}
