package org.eclipse.edc.protocol.dsp.controlplane;


import io.restassured.specification.RequestSpecification;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.junit.extensions.EdcExtension;
import org.eclipse.edc.protocol.dsp.controlplane.controller.TransferProcessController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static org.eclipse.edc.junit.testfixtures.TestUtils.getFreePort;
import static io.restassured.RestAssured.given;

@ExtendWith(EdcExtension.class)
public class DspTransferProcessIntegrationTest {

    private final int port = getFreePort();

    private final String authKey = "123456";

    @BeforeEach
    void setUp(EdcExtension extension){
        extension.setConfiguration(Map.of(
                "web.http.port", String.valueOf(getFreePort()),
                "web.http.path", "/api",
                "web.http.management.port", String.valueOf(getFreePort()),
                "web.http.management.path", "/api/v1/management",
                "web.http.protocol.port", String.valueOf(port),
                "web.http.protocol.path", "/api/v1/dsp",
                "edc.api.auth.key", authKey
        ));
    }

    @Test
    public void getTransferProcess(){

        baseRequest()
                .get("/transfer-processes/0")
                .then()
                .statusCode(200)
                .contentType("application/json");
    }

    private RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + port)
                .basePath("/api/v1/dsp")
                .when();
    }
}