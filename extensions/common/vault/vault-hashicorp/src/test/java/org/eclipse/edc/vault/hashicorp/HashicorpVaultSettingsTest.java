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

import org.eclipse.edc.participantcontext.spi.config.ParticipantContextConfig;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.vault.hashicorp.auth.HashicorpJwtTokenProvider;
import org.eclipse.edc.vault.hashicorp.auth.HashicorpVaultTokenProviderImpl;
import org.eclipse.edc.vault.hashicorp.client.HashicorpVaultConfig;
import org.eclipse.edc.vault.hashicorp.client.HashicorpVaultCredentials;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HashicorpVaultSettingsTest {

    private final ParticipantContextConfig participantContextConfig = mock();

    @Test
    void forParticipant() {
    }

    @Test
    void forParticipant_whenParticipantNotFound_shouldReturnFailure() {

        when(participantContextConfig.getSensitiveString(anyString(), anyString())).thenThrow(new EdcException("test exception"));
        assertThatThrownBy(() -> HashicorpVaultSettings.forParticipant("participant1", participantContextConfig))
                .isInstanceOf(EdcException.class)
                .hasMessage("test exception");
    }

    @ParameterizedTest
    @ValueSource(strings = { "  ", "\t", "\n" })
    @NullAndEmptySource
    void forParticipant_whenVaultConfigInvalid_returnsFailure(String vaultConfig) {
        when(participantContextConfig.getSensitiveString(anyString(), anyString())).thenReturn(vaultConfig);
        assertThat(HashicorpVaultSettings.forParticipant("participant1", participantContextConfig)).isNull();
    }

    @Test
    void forParticipant_whenVaultConfigEmpty_returnsEmptyConfig() {
        when(participantContextConfig.getSensitiveString(anyString(), anyString())).thenReturn("{}");

        assertThat(HashicorpVaultSettings.forParticipant("participant1", participantContextConfig))
                .isNotNull()
                .satisfies(settings -> {
                    assertThat(settings.config()).isNull();
                    assertThat(settings.credentials()).isNull();
                });
    }

    @Test
    void tokenProvider_whenVaultToken() {
        var creds = HashicorpVaultCredentials.Builder.newInstance()
                .token("some-vault-token")
                .build();
        var config = HashicorpVaultConfig.Builder.newInstance()
                .vaultUrl("http://example.com/vault")
                .build();
        assertThat(new HashicorpVaultSettings(config, creds).tokenProvider(mock())).isInstanceOf(HashicorpVaultTokenProviderImpl.class);
    }

    @Test
    void tokenProvider_whenJwtToken() {
        var creds = HashicorpVaultCredentials.Builder.newInstance()
                .clientId("client-id")
                .tokenUrl("http://example.com/token")
                .clientSecret("client-secret")
                .build();
        var config = HashicorpVaultConfig.Builder.newInstance()
                .vaultUrl("http://example.com/vault")
                .build();
        assertThat(new HashicorpVaultSettings(config, creds).tokenProvider(mock())).isInstanceOf(HashicorpJwtTokenProvider.class);
    }
}