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
import org.eclipse.edc.participantcontext.spi.config.ParticipantContextConfig;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.vault.hashicorp.client.HashicorpVaultConfig;
import org.eclipse.edc.vault.hashicorp.client.HashicorpVaultCredentials;

import static org.eclipse.edc.vault.hashicorp.VaultConstants.VAULT_CONFIG;

/**
 * POJO that contains a config object and a credentials object. This is intended to be used as DTO.
 */
public record HashicorpVaultSettings(HashicorpVaultConfig config, HashicorpVaultCredentials credentials) {

    private static final ObjectMapper MAPPER = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    public static HashicorpVaultSettings forParticipant(String participantContextId, ParticipantContextConfig config) {
        var vaultConfigJson = config.getString(participantContextId, VAULT_CONFIG);
        try {
            return MAPPER.readValue(vaultConfigJson, HashicorpVaultSettings.class);
        } catch (JsonProcessingException e) {
            throw new EdcException(e);
        }
    }
}
