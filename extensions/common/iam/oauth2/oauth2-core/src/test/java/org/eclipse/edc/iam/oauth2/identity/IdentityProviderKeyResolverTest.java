/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.edc.iam.oauth2.identity;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.eclipse.edc.iam.oauth2.jwt.JwkKeys;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.TypeManager;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.junit.testfixtures.TestUtils.testHttpClient;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IdentityProviderKeyResolverTest {

    private static final String JWKS_URL = "https://test.jwks.url";
    private static final String JWKS_FILE = "jwks_response.json";
    private final Interceptor interceptor = mock(Interceptor.class);
    private final TypeManager typeManager = new TypeManager();
    private final EdcHttpClient httpClient = testHttpClient(interceptor);
    private JwkKeys testKeys;
    private IdentityProviderKeyResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new IdentityProviderKeyResolver(mock(Monitor.class), httpClient, typeManager, new IdentityProviderKeyResolverConfiguration(JWKS_URL, 1));

        try (var stream = getClass().getClassLoader().getResourceAsStream(JWKS_FILE)) {
            testKeys = new ObjectMapper().readValue(stream, JwkKeys.class);
        } catch (IOException e) {
            throw new EdcException("Failed to load keys from file");
        }
    }

    @Test
    void getKeys_shouldGetUpdatedKeys() throws IOException {
        when(interceptor.intercept(any())).thenReturn(response(200, testKeys));

        var result = resolver.getKeys();

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent()).hasSize(5).containsKey("nOo3ZDrODXEK1jKWhXslHR_KXEg");
    }

    @Test
    void getKeys_shouldReturnFailureIfServerReturnsError() throws IOException {
        when(interceptor.intercept(any())).thenReturn(response(500, emptyMap()));

        var result = resolver.getKeys();

        assertThat(result.failed()).isTrue();
    }

    @Test
    void getKeys_shouldReturnFailureIfBodyCannotBeDeserialized() throws IOException {
        when(interceptor.intercept(any())).thenReturn(response(200, null));

        var result = resolver.getKeys();

        assertThat(result.failed()).isTrue();
    }

    @Test
    void getKeys_shouldReturnFailureIfNoKeysAreContainedInTheResult() throws IOException {
        when(interceptor.intercept(any())).thenReturn(response(200, Map.of("keys", emptyList())));

        var result = resolver.getKeys();

        assertThat(result.failed()).isTrue();
    }

    @Test
    void start_shouldThrowExceptionIfItFailsToLoadKeysTheFirstTime() throws IOException {
        when(interceptor.intercept(any())).thenReturn(response(500, emptyMap()));

        assertThatThrownBy(() -> resolver.start()).isInstanceOf(EdcException.class);
    }

    @NotNull
    private Response response(int code, Object body) {
        return new Response.Builder()
                .code(code)
                .body(ResponseBody.create(typeManager.writeValueAsString(body), MediaType.get("application/json")))
                .message("Test message")
                .protocol(Protocol.HTTP_1_1)
                .request(new Request.Builder().url("http://test.some.url").build())
                .build();
    }
}
