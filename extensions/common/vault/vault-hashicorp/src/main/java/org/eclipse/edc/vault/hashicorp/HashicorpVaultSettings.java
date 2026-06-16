/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.vault.hashicorp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.participantcontext.spi.config.ParticipantContextConfig;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.util.string.StringUtils;
import org.eclipse.edc.vault.hashicorp.client.HashicorpVaultConfig;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.vault.hashicorp.VaultConstants.VAULT_CONFIG;

/**
 * POJO that contains the per-partition vault configuration. This is intended to be serialized, e.g., as DTO in API calls.
 * Authentication is configured globally (see {@code HashicorpVaultAuthenticationExtension}); the only per-partition value
 * relevant to authentication is the partition key itself (the participant context id), which is derived from the
 * partition and used as the token-exchange {@code resource}.
 */
public record HashicorpVaultSettings(HashicorpVaultConfig config) {

    private static final ObjectMapper MAPPER = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    /**
     * Resolves the vault configuration from the {@link ParticipantContextConfig}. If no config is found for that participant, an exception is thrown.
     * If the participant context config is found, but does not contain a vault config, null is returned.
     *
     * @param participantContextId The participant context id.
     * @param config               The participant context config.
     * @return The vault configuration for the given participant context id, or null if not found.
     */
    public static @Nullable HashicorpVaultSettings forParticipant(String participantContextId, ParticipantContextConfig config) {
        var vaultConfigJson = config.getSensitiveString(participantContextId, VAULT_CONFIG);
        if (StringUtils.isNullOrBlank(vaultConfigJson)) {
            return null;
        }
        try {
            return MAPPER.readValue(vaultConfigJson, HashicorpVaultSettings.class);
        } catch (JsonProcessingException e) {
            throw new EdcException(e);
        }
    }

}
