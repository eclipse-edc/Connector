/*
 *  Copyright (c) 2022 Siemens AG
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Siemens AG - initial implementation
 *
 */

package org.eclipse.dataspaceconnector.spi.types.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HttpDataAddressTest {

    @Test
    void verifyGetProperties() {
        HttpDataAddress dataAddress = HttpDataAddress.Builder.newInstance()
                .name("name1")
                .baseUrl("http://myendpoint")
                .authCode("secret")
                .authKey("myKey")
                .secretName("mysecret")
                .addAdditionalHeader("Content-Type", "application/octet-stream")
                .addAdditionalHeader("x-ms-blob-type", "BlockBlob")
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
        assertEquals(2, dataAddress.getAdditionalHeaders().size());
        assertEquals("application/octet-stream", dataAddress.getAdditionalHeaders().get("Content-Type"));
        assertEquals("BlockBlob", dataAddress.getAdditionalHeaders().get("x-ms-blob-type"));
    }

    @Test
    void verifyGetDefaultValues() {
        HttpDataAddress dataAddress = HttpDataAddress.Builder.newInstance().build();

        assertEquals("HttpData", dataAddress.getType());
        assertEquals(0, dataAddress.getAdditionalHeaders().size());
    }
}