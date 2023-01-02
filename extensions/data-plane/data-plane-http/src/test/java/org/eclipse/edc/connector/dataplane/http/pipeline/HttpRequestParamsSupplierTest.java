/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *       Siemens - add chunked parameter
 *
 */

package org.eclipse.edc.connector.dataplane.http.pipeline;

import okhttp3.MediaType;
import org.eclipse.edc.connector.dataplane.http.testfixtures.HttpTestFixtures;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.HttpDataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.netty.handler.codec.http.HttpHeaders.Values.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HttpRequestParamsSupplierTest {

    private static final TypeManager TYPE_MANAGER = new TypeManager();

    private final Vault vault = mock(Vault.class);
    private TestHttpRequestParamsSupplier supplier;

    @BeforeEach
    public void setUp() {
        supplier = new TestHttpRequestParamsSupplier(vault, TYPE_MANAGER);
    }

    @Test
    void verifyExceptionThrownIfBaseUrlMissing() {
        var dataAddress = HttpDataAddress.Builder.newInstance().build();
        var request = createRequest(dataAddress);

        assertThatExceptionOfType(EdcException.class).isThrownBy(() -> supplier.apply(request));
    }

    @Test
    void verifySecretFromAddressIsUsed() {
        var dataAddress = HttpDataAddress.Builder.newInstance()
                .authKey("test-auth-key")
                .authCode("test-auth-code")
                .baseUrl("http://some.base.url")
                .build();
        var request = createRequest(dataAddress);

        var httpRequest = supplier.apply(request).toRequest();

        var headers = httpRequest.headers();
        assertThat(headers)
                .isNotNull()
                .hasSize(1);
        assertThat(headers.get(dataAddress.getAuthKey()))
                .isNotNull()
                .isEqualTo(dataAddress.getAuthCode());
    }

    @Test
    void verifySecretIsRetrievedFromVault() {
        var secretName = "test-secret-name";
        var secret = "test-secret";
        var dataAddress = HttpDataAddress.Builder.newInstance()
                .authKey("test-auth-key")
                .secretName(secretName)
                .baseUrl("http://test.base.url")
                .build();
        var request = createRequest(dataAddress);

        when(vault.resolveSecret(secretName)).thenReturn(secret);

        var httpRequest = supplier.apply(request).toRequest();

        var headers = httpRequest.headers();
        assertThat(headers)
                .isNotNull()
                .hasSize(1);
        assertThat(headers.get(dataAddress.getAuthKey()))
                .isNotNull()
                .isEqualTo(secret);

        verify(vault).resolveSecret(anyString());
    }

    @Test
    void verifySecretIsRetrievedFromVaultAsJson() {
        var secretName = "test-secret-name";
        var secret = "test-secret";
        var dataAddress = HttpDataAddress.Builder.newInstance()
                .authKey("test-auth-key")
                .secretName(secretName)
                .baseUrl("http://test.base.url")
                .build();
        var request = createRequest(dataAddress);
        when(vault.resolveSecret(secretName)).thenReturn(asJson(Map.of("token", secret)));

        var httpRequest = supplier.apply(request).toRequest();

        var headers = httpRequest.headers();
        assertThat(headers)
                .isNotNull()
                .hasSize(1);
        assertThat(headers.get(dataAddress.getAuthKey()))
                .isNotNull()
                .isEqualTo(secret);
    }

    @Test
    void throwsExceptionIfNoSecretNameIsSpecified() {
        var dataAddress = HttpDataAddress.Builder.newInstance()
                .authKey("test-auth-key")
                .baseUrl("http://test.base.url")
                .build();
        var request = createRequest(dataAddress);

        assertThatExceptionOfType(EdcException.class).isThrownBy(() -> supplier.apply(request));
    }

    @Test
    void throwsExceptionIfNoSecretIsFoundInVault() {
        var secretName = "test-secret-name";
        var dataAddress = HttpDataAddress.Builder.newInstance()
                .authKey("test-auth-key")
                .baseUrl("http://test.base.url")
                .secretName(secretName)
                .build();
        var request = createRequest(dataAddress);
        when(vault.resolveSecret(secretName)).thenReturn(null);

        assertThatExceptionOfType(EdcException.class).isThrownBy(() -> supplier.apply(request));
    }

    @Test
    void throwsExceptionIfNoSecretIsFoundInVaultAsJson() {
        var secretName = "test-secret-name";
        var dataAddress = HttpDataAddress.Builder.newInstance()
                .authKey("test-auth-key")
                .baseUrl("http://test.base.url")
                .secretName(secretName)
                .build();
        var request = createRequest(dataAddress);
        when(vault.resolveSecret(secretName)).thenReturn(asJson(Map.of("not-token-key", "anything")));

        assertThatExceptionOfType(EdcException.class).isThrownBy(() -> supplier.apply(request));
    }

    @Test
    void verifyAdditionalHeadersAreRetrievedFromAddress() {
        var additionalHeaders = Map.of("key1", "value1");
        var builder = HttpDataAddress.Builder.newInstance()
                .baseUrl("http://some.base.url");
        additionalHeaders.forEach(builder::addAdditionalHeader);
        var request = createRequest(builder.build());

        var httpRequest = supplier.apply(request).toRequest();

        var headers = httpRequest.headers();
        assertThat(headers)
                .isNotNull()
                .hasSize(1);
        additionalHeaders.forEach((s, s2) -> assertThat(headers.get(s)).isNotNull().isEqualTo(s2));
    }

    @Test
    void verifyAbstractMethodsInvokation() throws IOException {
        var dataAddress = HttpDataAddress.Builder.newInstance()
                .baseUrl("http://some.base.url")
                .build();
        var request = createRequest(dataAddress);

        var httpRequest = supplier.apply(request).toRequest();

        assertThat(httpRequest.url().url()).hasToString(dataAddress.getBaseUrl() + "/" + supplier.path + "?" + supplier.getQueryParamsString());
        var body = httpRequest.body();
        assertThat(body).isNotNull();
        assertThat(body.contentType()).isEqualTo(MediaType.get(supplier.contentType));
        assertThat(HttpTestFixtures.formatRequestBodyAsString(body)).isEqualTo(supplier.body);
        assertThat(httpRequest.method()).isEqualTo(supplier.method);
        assertThat(body.contentLength()).isEqualTo(-1L);
    }

    @Test
    void verifyChunkedCall() throws IOException {
        var dataAddress = HttpDataAddress.Builder.newInstance()
                .baseUrl("http://some.base.url")
                .build();
        var request = createRequest(dataAddress);

        var supplier = new TestHttpRequestParamsSupplier(vault, true, TYPE_MANAGER);
        var httpRequest = supplier.apply(request).toRequest();

        assertThat(httpRequest.url().url()).hasToString(dataAddress.getBaseUrl() + "/" + supplier.path + "?" + supplier.getQueryParamsString());
        var body = httpRequest.body();
        assertThat(body).isNotNull();
        assertThat(body.contentType()).isEqualTo(MediaType.get(supplier.contentType));
        assertThat(HttpTestFixtures.formatRequestBodyAsString(body)).isEqualTo(supplier.body);
        assertThat(httpRequest.method()).isEqualTo(supplier.method);
        assertThat(body.contentLength()).isEqualTo(supplier.body.getBytes().length);
    }

    private String asJson(Map<String, String> map) {
        return TYPE_MANAGER.writeValueAsString(map);
    }

    private DataFlowRequest createRequest(DataAddress source) {
        return DataFlowRequest.Builder.newInstance()
                .destinationDataAddress(DataAddress.Builder.newInstance().type("test-type").build())
                .sourceDataAddress(source)
                .processId(UUID.randomUUID().toString())
                .build();
    }

    public static final class TestHttpRequestParamsSupplier extends HttpRequestParamsSupplier {

        private final String method;
        private final String path;
        private final Map<String, String> queryParams;
        private final String contentType;
        private final String body;
        private final boolean isOneGo;

        private TestHttpRequestParamsSupplier(Vault vault, TypeManager typeManager) {
            this(vault, false, typeManager);
        }

        private TestHttpRequestParamsSupplier(Vault vault, boolean isOneGo, TypeManager typeManager) {
            super(vault, typeManager);
            this.method = new Random().nextBoolean() ? "PUT" : "POST";
            this.isOneGo = isOneGo;
            this.path = "somepath";
            this.queryParams = Map.of("foo", "bar", "hello", "world");
            this.contentType = new Random().nextBoolean() ? APPLICATION_JSON : APPLICATION_X_WWW_FORM_URLENCODED;
            this.body = "Test-Body";
        }

        @Override
        protected boolean extractNonChunkedTransfer(HttpDataAddress address) {
            return isOneGo;
        }

        @Override
        protected @NotNull DataAddress selectAddress(DataFlowRequest request) {
            return request.getSourceDataAddress();
        }

        @Override
        protected String extractMethod(HttpDataAddress address, DataFlowRequest request) {
            return method;
        }

        @Override
        protected @Nullable String extractPath(HttpDataAddress address, DataFlowRequest request) {
            return path;
        }

        @Override
        protected @NotNull Map<String, String> extractQueryParams(HttpDataAddress address, DataFlowRequest request) {
            return queryParams;
        }

        @Override
        protected @Nullable String extractContentType(HttpDataAddress address, DataFlowRequest request) {
            return contentType;
        }

        @Override
        protected @Nullable String extractBody(HttpDataAddress address, DataFlowRequest request) {
            return body;
        }

        public String getQueryParamsString() {
            return queryParams.entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .collect(Collectors.joining("&"));
        }
    }
}
