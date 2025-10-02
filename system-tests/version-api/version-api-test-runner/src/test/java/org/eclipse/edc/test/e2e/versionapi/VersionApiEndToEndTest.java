/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.test.e2e.versionapi;

import io.restassured.common.mapper.TypeRef;
import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneClientFactory;
import org.eclipse.edc.connector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.eclipse.edc.spi.system.apiversion.VersionRecord;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.util.io.Ports;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;

public class VersionApiEndToEndTest {

    public static final int DEFAULT_CONTEXT_PORT = Ports.getFreePort();

    @RegisterExtension
    public static final RuntimeExtension RUNTIME = new RuntimePerClassExtension(new EmbeddedRuntime("control-plane", ":system-tests:version-api:version-api-test-runtime")
            .configurationProvider(() -> ConfigFactory.fromMap(new HashMap<>() {
                {
                    put("web.http.path", "/");
                    put("web.http.port", String.valueOf(DEFAULT_CONTEXT_PORT));
                    put("web.http.control.port", String.valueOf(Ports.getFreePort()));
                    put("web.http.management.path", "/management");
                    put("web.http.management.port", String.valueOf(8181));
                    var dspPort = Ports.getFreePort();
                    put("web.http.protocol.path", "/protocol");
                    put("web.http.protocol.port", String.valueOf(dspPort));
                    put("edc.dsp.callback.address", "http://localhost:" + dspPort + "/protocol");
                }
            }))
            .registerServiceMock(DataPlaneManager.class, mock())
            .registerServiceMock(DataPlaneClientFactory.class, mock()));

    @Test
    void getVersion() {
        var result = given()
                .port(DEFAULT_CONTEXT_PORT)
                .when()
                .get("/v1/version")
                .then()
                .statusCode(200)
                .body(notNullValue())
                .extract().body().as(new TypeRef<Map<String, List<VersionRecord>>>() {
                });

        assertThat(result).containsKeys("management", "version", "control", "observability");
        assertThat(result.get("management")).hasSize(2)
                .anyMatch(vr -> vr.version().startsWith("3.") && vr.maturity().equals("stable"))
                .anyMatch(vr -> vr.version().equals("4.0.0-alpha") && vr.maturity().equals("alpha"));
        assertThat(result.get("version")).hasSize(1);
        assertThat(result.get("observability")).hasSize(1).anyMatch(vr -> vr.version().equals("1.0.0"));
    }

}
