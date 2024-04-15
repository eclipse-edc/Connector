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

package org.eclipse.edc.connector.dataplane.http.params;

import org.eclipse.edc.connector.dataplane.http.spi.HttpDataAddress;
import org.eclipse.edc.connector.dataplane.http.spi.HttpRequestParamsProvider;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HttpRequestParamsProviderImplTest {

    private static final TypeManager TYPE_MANAGER = new JacksonTypeManager();

    private final Vault vault = mock(Vault.class);
    private HttpRequestParamsProvider provider;

    @BeforeEach
    public void setUp() {
        provider = new HttpRequestParamsProviderImpl(vault, TYPE_MANAGER);
    }

    @Test
    void verifyExceptionThrownIfBaseUrlMissing() {
        var dataAddress = HttpDataAddress.Builder.newInstance().build();
        var request = createRequest(dataAddress);

        assertThatExceptionOfType(EdcException.class).isThrownBy(() -> provider.provideSinkParams(request));
        assertThatExceptionOfType(EdcException.class).isThrownBy(() -> provider.provideSourceParams(request));
    }

    @Test
    void verifySecretFromAddressIsUsed() {
        var dataAddress = HttpDataAddress.Builder.newInstance()
                .authKey("test-auth-key")
                .authCode("test-auth-code")
                .baseUrl("http://some.base.url")
                .build();
        var request = createRequest(dataAddress);

        var params = provider.provideSourceParams(request);

        assertThat(params.getHeaders()).containsEntry("test-auth-key", "test-auth-code");
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
        when(vault.resolveSecret(secretName)).thenReturn(secret);
        var request = createRequest(dataAddress);

        var params = provider.provideSourceParams(request);

        assertThat(params.getHeaders()).containsEntry("test-auth-key", "test-secret");
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

        var params = provider.provideSourceParams(request);

        assertThat(params.getHeaders()).containsEntry("test-auth-key", "test-secret");
    }

    @Test
    void throwsExceptionIfNoSecretNameIsSpecified() {
        var dataAddress = HttpDataAddress.Builder.newInstance()
                .authKey("test-auth-key")
                .baseUrl("http://test.base.url")
                .build();
        var request = createRequest(dataAddress);

        assertThatExceptionOfType(EdcException.class).isThrownBy(() -> provider.provideSinkParams(request));
        assertThatExceptionOfType(EdcException.class).isThrownBy(() -> provider.provideSourceParams(request));
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

        assertThatExceptionOfType(EdcException.class).isThrownBy(() -> provider.provideSinkParams(request));
        assertThatExceptionOfType(EdcException.class).isThrownBy(() -> provider.provideSourceParams(request));
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

        assertThatExceptionOfType(EdcException.class).isThrownBy(() -> provider.provideSinkParams(request));
        assertThatExceptionOfType(EdcException.class).isThrownBy(() -> provider.provideSourceParams(request));
    }

    @Test
    void verifyAdditionalHeadersAreRetrievedFromAddress() {
        var address = HttpDataAddress.Builder.newInstance()
                .baseUrl("http://some.base.url")
                .addAdditionalHeader("key1", "value1")
                .build();
        var request = createRequest(address);

        var params = provider.provideSourceParams(request);

        assertThat(params.getHeaders()).containsEntry("key1", "value1");
    }

    private String asJson(Map<String, String> map) {
        return TYPE_MANAGER.writeValueAsString(map);
    }

    private DataFlowStartMessage createRequest(DataAddress source) {
        return DataFlowStartMessage.Builder.newInstance()
                .destinationDataAddress(DataAddress.Builder.newInstance().type("test-type").build())
                .sourceDataAddress(source)
                .processId(UUID.randomUUID().toString())
                .build();
    }

}
