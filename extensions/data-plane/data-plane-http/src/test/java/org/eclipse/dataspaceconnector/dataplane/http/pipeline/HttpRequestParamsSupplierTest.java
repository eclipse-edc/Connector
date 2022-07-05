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
 *
 */

package org.eclipse.dataspaceconnector.dataplane.http.pipeline;

import com.github.javafaker.Faker;
import io.netty.handler.codec.http.HttpMethod;
import okhttp3.MediaType;
import org.eclipse.dataspaceconnector.dataplane.http.HttpTestFixtures;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.HttpDataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Map;
import java.util.Random;

import static io.netty.handler.codec.http.HttpHeaders.Values.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HttpRequestParamsSupplierTest {

    private static final Faker FAKER = new Faker();

    private final Vault vaultMock = Mockito.mock(Vault.class);
    private TestHttpRequestParamsSupplier supplier;

    @BeforeEach
    public void setUp() {
        supplier = new TestHttpRequestParamsSupplier(vaultMock);
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
                .authKey(FAKER.lorem().word())
                .authCode(FAKER.lorem().word())
                .baseUrl("http://" + FAKER.internet().url())
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
        var secretName = FAKER.lorem().word();
        var secret = FAKER.lorem().word();
        var dataAddress = HttpDataAddress.Builder.newInstance()
                .authKey(FAKER.lorem().word())
                .secretName(secretName)
                .baseUrl("http://" + FAKER.internet().url())
                .build();
        var request = createRequest(dataAddress);

        when(vaultMock.resolveSecret(secretName)).thenReturn(secret);

        var httpRequest = supplier.apply(request).toRequest();

        var headers = httpRequest.headers();
        assertThat(headers)
                .isNotNull()
                .hasSize(1);
        assertThat(headers.get(dataAddress.getAuthKey()))
                .isNotNull()
                .isEqualTo(secret);

        verify(vaultMock).resolveSecret(anyString());
    }

    @Test
    void verifyAdditionalHeadersAreRetrievedFromAddress() {
        var additionalHeaders = Map.of(FAKER.lorem().word(), FAKER.lorem().word());
        var builder = HttpDataAddress.Builder.newInstance()
                .baseUrl("http://" + FAKER.internet().url());
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
                .baseUrl("http://" + FAKER.internet().url())
                .build();
        var request = createRequest(dataAddress);

        var httpRequest = supplier.apply(request).toRequest();

        assertThat(httpRequest.url().url()).hasToString(dataAddress.getBaseUrl() + "/" + supplier.path + "?" + supplier.queryParams);
        var body = httpRequest.body();
        assertThat(body).isNotNull();
        assertThat(body.contentType()).isEqualTo(MediaType.get(supplier.contentType));
        assertThat(HttpTestFixtures.formatRequestBodyAsString(body)).isEqualTo(supplier.body);
        assertThat(httpRequest.method()).isEqualTo(supplier.method);
    }

    private static DataFlowRequest createRequest(DataAddress source) {
        return DataFlowRequest.Builder.newInstance()
                .destinationDataAddress(DataAddress.Builder.newInstance().type(FAKER.lorem().word()).build())
                .sourceDataAddress(source)
                .processId(FAKER.internet().uuid())
                .build();
    }

    public static final class TestHttpRequestParamsSupplier extends HttpRequestParamsSupplier {

        private final String method;
        private final String path;
        private final String queryParams;
        private final String contentType;
        private final String body;

        private TestHttpRequestParamsSupplier(Vault vault) {
            super(vault);
            this.method = new Random().nextBoolean() ? HttpMethod.PUT.name() : HttpMethod.POST.name();
            this.path = FAKER.lorem().word();
            this.queryParams = FAKER.lorem().word();
            this.contentType = new Random().nextBoolean() ? APPLICATION_JSON : APPLICATION_X_WWW_FORM_URLENCODED;
            this.body = FAKER.lorem().word();
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
        protected @Nullable String extractQueryParams(HttpDataAddress address, DataFlowRequest request) {
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
    }
}