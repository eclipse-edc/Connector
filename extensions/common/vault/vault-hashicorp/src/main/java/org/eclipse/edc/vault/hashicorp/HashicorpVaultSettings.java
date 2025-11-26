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
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.participantcontext.spi.config.ParticipantContextConfig;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.util.string.StringUtils;
import org.eclipse.edc.vault.hashicorp.auth.HashicorpJwtTokenProvider;
import org.eclipse.edc.vault.hashicorp.auth.HashicorpVaultTokenProviderImpl;
import org.eclipse.edc.vault.hashicorp.client.HashicorpVaultConfig;
import org.eclipse.edc.vault.hashicorp.client.HashicorpVaultCredentials;
import org.eclipse.edc.vault.hashicorp.spi.auth.HashicorpVaultTokenProvider;

import static org.eclipse.edc.vault.hashicorp.VaultConstants.VAULT_CONFIG;

/**
 * POJO that contains a config object and a credentials object. This is intended to be serialized, e.g., as DTO in API calls.
 */
public record HashicorpVaultSettings(HashicorpVaultConfig config, HashicorpVaultCredentials credentials) {

    private static final ObjectMapper MAPPER = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    public static HashicorpVaultSettings forParticipant(String participantContextId, ParticipantContextConfig config) {
        var vaultConfigJson = config.getString(participantContextId, VAULT_CONFIG);
        if (StringUtils.isNullOrBlank(vaultConfigJson)) {
            throw new EdcException("No vault configuration found for participant context '%s'".formatted(participantContextId));
        }
        try {
            return MAPPER.readValue(vaultConfigJson, HashicorpVaultSettings.class);
        } catch (JsonProcessingException e) {
            throw new EdcException(e);
        }
    }

    public HashicorpVaultTokenProvider tokenProvider(EdcHttpClient edcHttpClient) {
        if (credentials.getToken() != null) {
            return new HashicorpVaultTokenProviderImpl(credentials.getToken());
        }

        return HashicorpJwtTokenProvider.Builder.newInstance()
                .clientId(credentials.getClientId())
                .clientSecret(credentials.getClientSecret())
                .tokenUrl(credentials.getTokenUrl())
                .vaultUrl(config.getVaultUrl())
                .httpClient(edcHttpClient)
                .build();
    }

}
