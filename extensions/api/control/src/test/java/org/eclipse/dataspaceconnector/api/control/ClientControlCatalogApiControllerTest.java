package org.eclipse.dataspaceconnector.api.control;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

class ClientControlCatalogApiControllerTest extends AbstractClientControlCatalogApiControllerTest {
    private static final String CONNECTOR_ID = UUID.randomUUID().toString();

    @Override
    protected Map<String, String> getSystemProperties() {
        return new HashMap<>() {
            {
                put("web.http.port", String.valueOf(getPort()));
                put("edc.ids.id", "urn:connector:" + CONNECTOR_ID);
            }
        };
    }

    @BeforeEach
    void setUp() {

    }

    @Test
    void test() {
        String requestUri = String.format("%s%s", getUrl(), String.format("/api/control/catalog?provider=%s/api/ids/multipart", getUrl()));

        JsonPath jsonPath = RestAssured.given()
                .when()
                .get(requestUri)
                .then()
                .statusCode(200)
                .extract()
                .jsonPath();

        Assertions.assertEquals("urn:catalog:default", jsonPath.getString("id"));
        Assertions.assertEquals("urn:contractoffer:1", jsonPath.get("contractOffers[0].id"));
        Assertions.assertNotNull(jsonPath.get("contractOffers[0].policy.uid"));
        Assertions.assertEquals("dataspaceconnector:permission", jsonPath.get("contractOffers[0].policy.permissions[0].edctype"));
        Assertions.assertEquals("1", jsonPath.get("contractOffers[0].policy.permissions[0].target"));
        Assertions.assertEquals("USE", jsonPath.get("contractOffers[0].policy.permissions[0].action.type"));
        Assertions.assertEquals("set", jsonPath.get("contractOffers[0].policy.@type.@policytype"));
        Assertions.assertEquals(1234, jsonPath.getInt("contractOffers[0].assets[0].properties['ids:byteSize']"));
        Assertions.assertEquals("1", jsonPath.get("contractOffers[0].assets[0].properties['asset:prop:id']"));
        Assertions.assertEquals("txt", jsonPath.get("contractOffers[0].assets[0].properties['ids:fileExtension']"));
        Assertions.assertEquals("filename1", jsonPath.get("contractOffers[0].assets[0].properties['ids:fileName']"));
        Assertions.assertEquals("urn:contractoffer:2", jsonPath.get("contractOffers[1].id"));
        Assertions.assertNotNull(jsonPath.get("contractOffers[1].policy.uid"));
        Assertions.assertEquals("dataspaceconnector:permission", jsonPath.get("contractOffers[1].policy.permissions[0].edctype"));
        Assertions.assertEquals("2", jsonPath.get("contractOffers[1].policy.permissions[0].target"));
        Assertions.assertEquals("USE", jsonPath.get("contractOffers[1].policy.permissions[0].action.type"));
        Assertions.assertEquals("set", jsonPath.get("contractOffers[1].policy.@type.@policytype"));
        Assertions.assertEquals(5678, jsonPath.getInt("contractOffers[1].assets[0].properties['ids:byteSize']"));
        Assertions.assertEquals("2", jsonPath.get("contractOffers[1].assets[0].properties['asset:prop:id']"));
        Assertions.assertEquals("pdf", jsonPath.get("contractOffers[1].assets[0].properties['ids:fileExtension']"));
        Assertions.assertEquals("filename2", jsonPath.get("contractOffers[1].assets[0].properties['ids:fileName']"));
    }
}