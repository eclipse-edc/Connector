/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.iam.identitytrust.core.defaults;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.eclipse.edc.identitytrust.model.CredentialFormat;
import org.eclipse.edc.identitytrust.model.CredentialSubject;
import org.eclipse.edc.identitytrust.model.Issuer;
import org.eclipse.edc.identitytrust.model.VerifiableCredential;
import org.eclipse.edc.identitytrust.model.VerifiablePresentation;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.util.JacksonJsonLd.createObjectMapper;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getResourceFileContentAsString;
import static org.eclipse.edc.spi.result.Result.success;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultCredentialServiceClientTest {

    private static final String CS_URL = "http://test.com/cs";
    private final EdcHttpClient httpClientMock = mock();
    private DefaultCredentialServiceClient client;

    @BeforeEach
    void setup() {
        var registry = mock(TypeTransformerRegistry.class);
        when(registry.transform(isA(JsonObject.class), eq(VerifiablePresentation.class)))
                .thenReturn(success(createPresentation()));
        var jsonLdMock = mock(JsonLd.class);
        when(jsonLdMock.expand(any())).thenAnswer(a -> success(a.getArgument(0)));
        client = new DefaultCredentialServiceClient(httpClientMock, Json.createBuilderFactory(Map.of()),
                createObjectMapper(), registry, jsonLdMock, mock());
    }

    @Test
    @DisplayName("CS returns a single LDP-VP")
    void requestPresentation_singleLdpVp() throws IOException {
        when(httpClientMock.execute(any()))
                .thenReturn(response(200, getResourceFileContentAsString("single_ldp-vp.json")));

        var result = client.requestPresentation(CS_URL, "foo", List.of());
        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent()).hasSize(1).allMatch(vpc -> vpc.format() == CredentialFormat.JSON_LD);
    }

    @Test
    @DisplayName("CS returns a single JWT-VP")
    void requestPresentation_singleJwtVp() throws IOException {
        when(httpClientMock.execute(any()))
                .thenReturn(response(200, getResourceFileContentAsString("single_jwt-vp.json")));

        var result = client.requestPresentation(CS_URL, "foo", List.of());
        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent()).hasSize(1).allMatch(vpc -> vpc.format() == CredentialFormat.JWT);
    }

    @Test
    @DisplayName("CS returns multiple VPs, one LDP-VP and a JWT-VP")
    void requestPresentationLdp_multipleVp_mixed() throws IOException {
        when(httpClientMock.execute(any()))
                .thenReturn(response(200, getResourceFileContentAsString("multiple_vp-token_mixed.json")));

        var result = client.requestPresentation(CS_URL, "foo", List.of());
        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent()).hasSize(2)
                .anySatisfy(vp -> assertThat(vp.format()).isEqualTo(CredentialFormat.JSON_LD))
                .anySatisfy(vp -> assertThat(vp.format()).isEqualTo(CredentialFormat.JWT));
    }

    @Test
    @DisplayName("CS returns multiple LDP-VPs")
    void requestPresentation_mulipleVp_onlyLdp() throws IOException {
        when(httpClientMock.execute(any()))
                .thenReturn(response(200, getResourceFileContentAsString("multiple_vp-token_ldp.json")));

        var result = client.requestPresentation(CS_URL, "foo", List.of());
        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent()).hasSize(2)
                .allSatisfy(vp -> assertThat(vp.format()).isEqualTo(CredentialFormat.JSON_LD));
    }

    @Test
    @DisplayName("CS returns multiple JWT-VPs")
    void requestPresentation_mulipleVp_onlyJwt() throws IOException {
        when(httpClientMock.execute(any()))
                .thenReturn(response(200, getResourceFileContentAsString("multiple_vp-token_jwt.json")));

        var result = client.requestPresentation(CS_URL, "foo", List.of());
        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent()).hasSize(2)
                .allSatisfy(vp -> assertThat(vp.format()).isEqualTo(CredentialFormat.JWT));
    }

    @ParameterizedTest(name = "CS returns HTTP error code {0}")
    @ValueSource(ints = { 400, 401, 403, 503, 501 })
    void requestPresentation_csReturnsError(int httpCode) throws IOException {
        when(httpClientMock.execute(any()))
                .thenReturn(response(httpCode, "Test failure"));

        var res = client.requestPresentation(CS_URL, "foo", List.of());
        assertThat(res.failed()).isTrue();
        assertThat(res.getFailureDetail()).isEqualTo("Presentation Query failed: HTTP %s, message: Test failure".formatted(httpCode));
    }

    private VerifiablePresentation createPresentation() {
        return VerifiablePresentation.Builder.newInstance()
                .type("VerifiablePresentation")
                .credential(VerifiableCredential.Builder.newInstance()
                        .issuer(new Issuer("test-issuer", Map.of()))
                        .type("VerifiableCredential")
                        .issuanceDate(Instant.now())
                        .credentialSubject(CredentialSubject.Builder.newInstance()
                                .id("test-subject")
                                .claim("foo", "bar")
                                .build())
                        .build())
                .build();
    }

    private Response response(int code, String body) {
        return new Response.Builder()
                .request(mock())
                .protocol(Protocol.HTTP_2)
                .code(code) // status code
                .message("")
                .body(ResponseBody.create(
                        body,
                        MediaType.get("application/json; charset=utf-8")
                ))
                .build();
    }


}