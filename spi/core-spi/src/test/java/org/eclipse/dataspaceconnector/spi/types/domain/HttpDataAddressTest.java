package org.eclipse.dataspaceconnector.spi.types.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpDataAddressTest {

    @Test
    void verifyGetProperties() {
        HttpDataAddress dataAddress = HttpDataAddress.Builder.newInstance()
                .name("name1")
                .baseUrl("http://myendpoint")
                .authCode("secret")
                .authKey("myKey")
                .secretName("mysecret")
                .httpVerb("PUT")
                .additionalHeaders("{\"Content-Type\" : \"application/octet-stream\",\"x-ms-blob-type\": \"BlockBlob\"}")
                .proxyBody("proxyBody1")
                .proxyMethod("proxyMethod1")
                .proxyPath("proxyPath1")
                .proxyQueryParams("proxyQueryParams1")
                .build();

        assertEquals("HttpData", dataAddress.getType());
        assertEquals("name1", dataAddress.getName());
        assertEquals("http://myendpoint", dataAddress.getBaseUrl());
        assertEquals("myKey", dataAddress.getAuthKey());
        assertEquals("secret", dataAddress.getAuthCode());
        assertEquals("proxyBody1", dataAddress.getProxyBody());
        assertEquals("proxyMethod1", dataAddress.getProxyMethod());
        assertEquals("proxyPath1", dataAddress.getProxyPath());
        assertEquals("proxyQueryParams1", dataAddress.getProxyQueryParams());
        assertEquals("mysecret", dataAddress.getSecretName());
        assertEquals("PUT", dataAddress.getHttpVerb());
        assertEquals(2, dataAddress.getAdditionalHeaders().size());
        assertEquals("application/octet-stream", dataAddress.getAdditionalHeaders().get("Content-Type"));
        assertEquals("BlockBlob", dataAddress.getAdditionalHeaders().get("x-ms-blob-type"));
    }

    @Test
    void verifyGetDefaultValues() {
        HttpDataAddress dataAddress = HttpDataAddress.Builder.newInstance().build();

        assertEquals("HttpData", dataAddress.getType());
        assertEquals("POST", dataAddress.getHttpVerb());
        assertEquals(0, dataAddress.getAdditionalHeaders().size());
    }
}