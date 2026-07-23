/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.vault.hashicorp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import dev.failsafe.RetryPolicy;
import okhttp3.OkHttpClient;
import org.eclipse.edc.http.client.EdcHttpClientImpl;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.participantcontext.spi.config.ParticipantContextConfig;
import org.eclipse.edc.vault.hashicorp.client.HashicorpVaultConfig;
import org.eclipse.edc.vault.hashicorp.spi.auth.HashicorpVaultTokenProvider;
import org.eclipse.edc.vault.hashicorp.spi.auth.HashicorpVaultTokenProviderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.vault.hashicorp.VaultConstants.VAULT_CONFIG;
import static org.eclipse.edc.vault.hashicorp.VaultConstants.VAULT_TOKEN_HEADER;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ComponentTest
class HashicorpVaultSignatureServiceTest {

    private static final String DEFAULT_TOKEN = "default-token";
    private static final String PARTITION_TOKEN = "partition-token";
    private static final String KEY = "signing-key";

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private final ParticipantContextConfig participantContextConfig = mock();
    private final HashicorpVaultTokenProvider defaultTokenProvider = () -> DEFAULT_TOKEN;
    private final HashicorpVaultTokenProvider partitionTokenProvider = () -> PARTITION_TOKEN;
    private final HashicorpVaultTokenProviderFactory tokenProviderFactory =
            resource -> resource == null ? defaultTokenProvider : partitionTokenProvider;
    private final EdcHttpClient httpClient = new EdcHttpClientImpl(new OkHttpClient(), RetryPolicy.ofDefaults(), mock());
    private final ObjectMapper mapper = new ObjectMapper();
    private HashicorpVaultConfig defaultConfig;
    private HashicorpVaultSignatureService service;

    @BeforeEach
    void setup() {
        defaultConfig = HashicorpVaultConfig.Builder.newInstance()
                .vaultUrl(wireMock.baseUrl())
                .healthCheckPath("/healthcheck")
                .ttl(10)
                .build();
        service = new HashicorpVaultSignatureService(mock(), participantContextConfig, defaultConfig, httpClient, mapper, tokenProviderFactory);
    }

    @Test
    void sign_forDefault_usesDefaultConfigAndToken() {
        wireMock.stubFor(post(urlPathEqualTo("/v1/transit/sign/" + KEY))
                .willReturn(okJson("""
                        { "data": { "signature": "vault:v1:abc" } }
                        """)));

        assertThat(service.sign(KEY, "payload".getBytes(), "")).isSucceeded();

        verifyNoInteractions(participantContextConfig);
        wireMock.verify(postRequestedFor(urlPathEqualTo("/v1/transit/sign/" + KEY))
                .withHeader(VAULT_TOKEN_HEADER, equalTo(DEFAULT_TOKEN)));
    }

    @Test
    void sign_forPartition_usesPartitionConfigAndToken() {
        wireMock.stubFor(post(urlPathEqualTo("/v1/transit/sign/" + KEY))
                .willReturn(okJson("""
                        { "data": { "signature": "vault:v1:abc" } }
                        """)));

        when(participantContextConfig.getSensitiveString("partition1", VAULT_CONFIG))
                .thenReturn(asJson(new HashicorpVaultSettings(defaultConfig)));

        assertThat(service.sign("partition1", KEY, "payload".getBytes(), "")).isSucceeded();

        verify(participantContextConfig).getSensitiveString("partition1", VAULT_CONFIG);
        wireMock.verify(postRequestedFor(urlPathEqualTo("/v1/transit/sign/" + KEY))
                .withHeader(VAULT_TOKEN_HEADER, equalTo(PARTITION_TOKEN)));
    }

    @Test
    void sign_forPartition_whenConfigNotFound_fallsBackToDefault() {
        wireMock.stubFor(post(urlPathEqualTo("/v1/transit/sign/" + KEY))
                .willReturn(okJson("""
                        { "data": { "signature": "vault:v1:abc" } }
                        """)));

        when(participantContextConfig.getSensitiveString("partition1", VAULT_CONFIG)).thenReturn(null);

        assertThat(service.sign("partition1", KEY, "payload".getBytes(), "")).isSucceeded();

        verify(participantContextConfig).getSensitiveString("partition1", VAULT_CONFIG);
        wireMock.verify(postRequestedFor(urlPathEqualTo("/v1/transit/sign/" + KEY))
                .withHeader(VAULT_TOKEN_HEADER, equalTo(DEFAULT_TOKEN)));
    }

    @Test
    void sign_forPartition_whenConfigNotFoundAndNoFallback_throws() {
        defaultConfig = HashicorpVaultConfig.Builder.newInstance()
                .vaultUrl(wireMock.baseUrl())
                .healthCheckPath("/healthcheck")
                .allowFallback(false)
                .ttl(10)
                .build();
        service = new HashicorpVaultSignatureService(mock(), participantContextConfig, defaultConfig, httpClient, mapper, tokenProviderFactory);

        when(participantContextConfig.getSensitiveString("partition1", VAULT_CONFIG)).thenReturn(null);

        assertThatThrownBy(() -> service.sign("partition1", KEY, "payload".getBytes(), ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("falling back to the default vault is not allowed");

        wireMock.verify(0, postRequestedFor(urlPathEqualTo("/v1/transit/sign/" + KEY)));
    }

    @Test
    void verify_forPartition_usesPartitionConfigAndToken() {
        wireMock.stubFor(post(urlPathEqualTo("/v1/transit/verify/" + KEY))
                .willReturn(okJson("""
                        { "data": { "valid": true } }
                        """)));

        when(participantContextConfig.getSensitiveString("partition1", VAULT_CONFIG))
                .thenReturn(asJson(new HashicorpVaultSettings(defaultConfig)));

        assertThat(service.verify("partition1", KEY, "input".getBytes(), "vault:v1:abc".getBytes(), "")).isSucceeded();

        wireMock.verify(postRequestedFor(urlPathEqualTo("/v1/transit/verify/" + KEY))
                .withHeader(VAULT_TOKEN_HEADER, equalTo(PARTITION_TOKEN)));
    }

    @Test
    void rotate_forPartition_usesPartitionConfigAndToken() {
        wireMock.stubFor(post(urlPathEqualTo("/v1/transit/keys/" + KEY + "/rotate"))
                .willReturn(okJson("{}")));

        when(participantContextConfig.getSensitiveString("partition1", VAULT_CONFIG))
                .thenReturn(asJson(new HashicorpVaultSettings(defaultConfig)));

        assertThat(service.rotate("partition1", KEY, Map.of())).isSucceeded();

        wireMock.verify(postRequestedFor(urlPathEqualTo("/v1/transit/keys/" + KEY + "/rotate"))
                .withHeader(VAULT_TOKEN_HEADER, equalTo(PARTITION_TOKEN)));
    }

    private String asJson(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
