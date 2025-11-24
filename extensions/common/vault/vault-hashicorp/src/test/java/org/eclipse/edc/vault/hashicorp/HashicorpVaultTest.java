/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
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
import org.eclipse.edc.participantcontext.spi.config.ParticipantContextConfig;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.vault.hashicorp.client.HashicorpVaultConfig;
import org.eclipse.edc.vault.hashicorp.client.HashicorpVaultCredentials;
import org.eclipse.edc.vault.hashicorp.spi.auth.HashicorpVaultTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.status;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HashicorpVaultTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private final ParticipantContextConfig config = mock();
    private final HashicorpVaultTokenProvider tokenProvider = mock();
    private final EdcHttpClient httpClient = new EdcHttpClientImpl(new OkHttpClient(), RetryPolicy.ofDefaults(), mock());
    private final ObjectMapper mapper = new ObjectMapper();
    private HashicorpVault vault;

    @BeforeEach
    void setup() {
        var vaultConfig = HashicorpVaultConfig.Builder.newInstance()
                .vaultUrl(wireMock.baseUrl())
                .folderPath("baz")
                .secretPath("v1/secret")
                .healthCheckPath("/healthcheck")
                .ttl(10)
                .build();

        when(tokenProvider.vaultToken()).thenReturn("root");

        vault = new HashicorpVault(config, mock(), vaultConfig, tokenProvider, httpClient);


    }

    @Test
    void resolveSecret_forDefault() {
        wireMock.stubFor(get(urlPathMatching("/v1/secret/data/baz/foo"))
                .willReturn(okJson("""
                        {
                            "data": {
                                "data": {
                                    "content": "bar"
                                }
                            }
                        }
                        """)));
        var secret = vault.resolveSecret("foo");
        assertThat(secret).isEqualTo("bar");
    }

    @Test
    void resolveSecret_forDefault_notFound() {
        wireMock.stubFor(get(urlPathMatching("/v1/secret/data/baz/bizz"))
                .willReturn(notFound()));
        var secret = vault.resolveSecret("bizz");
        assertThat(secret).isNull();
    }

    @Test
    void resolveSecret_forPartition() {
        wireMock.stubFor(get(urlPathMatching("/v1/participants/data/participant1/baz"))
                .willReturn(okJson("""
                        {
                            "data": {
                                "data": {
                                    "content": "bar"
                                }
                            }
                        }
                        """)));

        var vaultConfig = HashicorpVaultConfig.Builder.newInstance()
                .vaultUrl(wireMock.baseUrl())
                .folderPath("participant1/")
                .secretPath("v1/participants")
                .healthCheckPath("/healthcheck")
                .ttl(10)
                .build();

        var creds = HashicorpVaultCredentials.Builder.newInstance()
                .token("foo-token")
                .build();
        when(config.getString("partition1", "vaultConfig")).thenReturn(asJson(new HashicorpVaultSettings(vaultConfig, creds)));


        var secret = vault.resolveSecret("partition1", "baz");
        assertThat(secret).isEqualTo("bar");
    }

    @Test
    void resolveSecret_forPartition_notFound() {
        wireMock.stubFor(get(urlPathMatching("/v1/participants/data/participant1/bizz"))
                .willReturn(notFound()));
        var vaultConfig = HashicorpVaultConfig.Builder.newInstance()
                .vaultUrl(wireMock.baseUrl())
                .folderPath("participant1/")
                .secretPath("v1/participants")
                .healthCheckPath("/healthcheck")
                .ttl(10)
                .build();

        var creds = HashicorpVaultCredentials.Builder.newInstance()
                .token("foo-token")
                .build();
        when(config.getString("partition1", "vaultConfig")).thenReturn(asJson(new HashicorpVaultSettings(vaultConfig, creds)));


        var secret = vault.resolveSecret("partition1", "baz");
        assertThat(secret).isNull();
    }

    @Test
    void storeSecret_forDefault() {
        wireMock.stubFor(post(urlPathMatching("/v1/secret/data/.*"))
                .willReturn(okJson("{}")));

        assertThat(vault.storeSecret("foo", "bar").succeeded()).isTrue();
    }

    @Test
    void storeSecret_forPartition() {
        var vaultConfig = HashicorpVaultConfig.Builder.newInstance()
                .vaultUrl(wireMock.baseUrl())
                .folderPath("participant1/")
                .secretPath("v1/participants")
                .healthCheckPath("/healthcheck")
                .ttl(10)
                .build();

        var creds = HashicorpVaultCredentials.Builder.newInstance()
                .token("foo-token")
                .build();
        when(config.getString("partition1", "vaultConfig")).thenReturn(asJson(new HashicorpVaultSettings(vaultConfig, creds)));

        wireMock.stubFor(post(urlPathMatching("/v1/participants/data/participant1.*"))
                .willReturn(okJson("{}")));

        assertThat(vault.storeSecret("partition1", "foo", "bar")).isSucceeded();
    }

    @Test
    void deleteSecret_forDefault() {
        wireMock.stubFor(delete(urlPathMatching("/v1/secret/data/foo.*"))
                .willReturn(status(204)));

        assertThat(vault.deleteSecret("foo")).isSucceeded();
    }

    @Test
    void deleteSecret_forDefault_notFound() {
        wireMock.stubFor(delete(urlPathMatching("/v1/secret/data/foo.*"))
                .willReturn(notFound()));

        assertThat(vault.deleteSecret("foo")).isSucceeded();
    }

    @Test
    void deleteSecret_forPartition() {
        var vaultConfig = HashicorpVaultConfig.Builder.newInstance()
                .vaultUrl(wireMock.baseUrl())
                .folderPath("participant1/")
                .secretPath("v1/participants")
                .healthCheckPath("/healthcheck")
                .ttl(10)
                .build();

        var creds = HashicorpVaultCredentials.Builder.newInstance()
                .token("foo-token")
                .build();
        when(config.getString("participant1", "vaultConfig")).thenReturn(asJson(new HashicorpVaultSettings(vaultConfig, creds)));

        wireMock.stubFor(delete(urlPathMatching("/v1/participants/data/participant1/foo.*"))
                .willReturn(status(204)));

        assertThat(vault.deleteSecret("participant1", "foo")).isSucceeded();
    }

    @Test
    void deleteSecret_forPartition_notFound() {
        var vaultConfig = HashicorpVaultConfig.Builder.newInstance()
                .vaultUrl(wireMock.baseUrl())
                .folderPath("participant1/")
                .secretPath("v1/participants")
                .healthCheckPath("/healthcheck")
                .ttl(10)
                .build();

        var creds = HashicorpVaultCredentials.Builder.newInstance()
                .token("foo-token")
                .build();
        when(config.getString("participant1", "vaultConfig")).thenReturn(asJson(new HashicorpVaultSettings(vaultConfig, creds)));

        wireMock.stubFor(delete(urlPathMatching("/v1/participants/data/participant1/foo.*"))
                .willReturn(notFound()));

        assertThat(vault.deleteSecret("participant1", "foo")).isSucceeded();
    }

    @Test
    void anyRequest_whenPartitionNotFound_shouldReturnFailure() {
        when(config.getString(anyString(), anyString())).thenThrow(new EdcException("test exception"));
        wireMock.stubFor(delete(urlPathMatching("/v1/participants/data/participant1/foo.*"))
                .willReturn(status(204)));

        assertThatThrownBy(() -> vault.deleteSecret("participant1", "foo")).isInstanceOf(EdcException.class).hasMessage("test exception");
    }

    private String asJson(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}