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

import static org.assertj.core.api.Assertions.assertThat;


class HttpDataAddressTest {

    @Test
    void verifyGetProperties() {
        HttpDataAddress dataAddress = HttpDataAddress.Builder.newInstance()
                .name("name1")
                .baseUrl("http://myendpoint")
                .authCode("secret")
                .authKey("myKey")
                .secretName("mysecret")
                .contentType("application/octet-stream")
                .addAdditionalHeader("Content-Type", "text/html; charset=UTF-8")
                .addAdditionalHeader("Keep-Alive", "timeout=5, max=1000")
                .addAdditionalHeader("x-ms-blob-type", "BlockBlob")
                .proxyBody("proxyBody1")
                .proxyMethod("proxyMethod1")
                .proxyPath("proxyPath1")
                .proxyQueryParams("proxyQueryParams1")
                .build();

        assertThat(dataAddress.getType()).isEqualTo("HttpData");
        assertThat(dataAddress.getName()).isEqualTo("name1");
        assertThat(dataAddress.getBaseUrl()).isEqualTo("http://myendpoint");
        assertThat(dataAddress.getAuthKey()).isEqualTo("myKey");
        assertThat(dataAddress.getAuthCode()).isEqualTo("secret");
        assertThat(dataAddress.getProxyBody()).isEqualTo("proxyBody1");
        assertThat(dataAddress.getProxyMethod()).isEqualTo("proxyMethod1");
        assertThat(dataAddress.getProxyPath()).isEqualTo("proxyPath1");
        assertThat(dataAddress.getProxyQueryParams()).isEqualTo("proxyQueryParams1");
        assertThat(dataAddress.getSecretName()).isEqualTo("mysecret");
        assertThat(dataAddress.getContentType()).isEqualTo("application/octet-stream");
        assertThat(dataAddress.getAdditionalHeaders().size()).isEqualTo(2);
        assertThat(dataAddress.getAdditionalHeaders().get("Keep-Alive")).isEqualTo("timeout=5, max=1000");
        assertThat(dataAddress.getAdditionalHeaders().get("x-ms-blob-type")).isEqualTo("BlockBlob");
    }

    @Test
    void verifyGetDefaultValues() {
        HttpDataAddress dataAddress = HttpDataAddress.Builder.newInstance().build();

        assertThat(dataAddress.getType()).isEqualTo("HttpData");
        assertThat(dataAddress.getAdditionalHeaders().size()).isEqualTo(0);
        assertThat(dataAddress.getContentType()).isEqualTo("application/octet-stream");
    }
}