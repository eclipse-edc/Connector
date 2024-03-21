/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.core.edr.defaults;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.edr.spi.store.EndpointDataReferenceCache;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.util.Optional;

/**
 * Vault implementation of {@link EndpointDataReferenceCache}
 */
public class VaultEndpointDataReferenceCache implements EndpointDataReferenceCache {

    public static final String SEPARATOR = "--";
    public static final String VAULT_PREFIX = "edr" + SEPARATOR;

    private final Vault vault;
    private final String vaultPath;

    private final ObjectMapper objectMapper;

    public VaultEndpointDataReferenceCache(Vault vault, String vaultPath, ObjectMapper objectMapper) {
        this.vault = vault;
        this.vaultPath = vaultPath;
        this.objectMapper = objectMapper;
    }

    @Override
    public StoreResult<DataAddress> get(String transferProcessId) {
        var key = toKey(transferProcessId);
        return Optional.ofNullable(vault.resolveSecret(key))
                .map(this::fromJson)
                .map(StoreResult::success)
                .orElse(StoreResult.notFound("EDR not found in the vault for transfer process: %s".formatted(transferProcessId)));
    }

    @Override
    public StoreResult<Void> put(String transferProcessId, DataAddress edr) {
        var result = vault.storeSecret(toKey(transferProcessId), toJson(edr));
        if (result.failed()) {
            return StoreResult.generalError(result.getFailureDetail());
        }
        return StoreResult.success();
    }

    @Override
    public StoreResult<Void> delete(String transferProcessId) {
        var result = vault.deleteSecret(toKey(transferProcessId));
        if (result.failed()) {
            return StoreResult.generalError(result.getFailureDetail());
        }
        return StoreResult.success();
    }

    private String toJson(DataAddress dataAddress) {
        try {
            return objectMapper.writeValueAsString(dataAddress);
        } catch (JsonProcessingException e) {
            throw new EdcPersistenceException(e);
        }
    }

    private DataAddress fromJson(String dataAddress) {
        try {
            return objectMapper.readValue(dataAddress, DataAddress.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private String toKey(String transferProcessId) {
        return vaultPath + VAULT_PREFIX + transferProcessId;
    }
}
