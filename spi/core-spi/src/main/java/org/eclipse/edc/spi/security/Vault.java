/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.spi.security;

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.Nullable;

/**
 * Provides secrets such as certificates and keys to the runtime.
 */
@ExtensionPoint
public interface Vault {

    /**
     * Resolve the secret for the given key.
     *
     * @param key the key
     * @return the key as a string or null if not found. Binary data will be Base 64 encoded.
     */
    @Nullable
    String resolveSecret(String key);

    /**
     * Saves a secret.
     *
     * @param key   the secret key
     * @param value the serialized secret value
     */
    Result<Void> storeSecret(String key, String value);

    /**
     * Deletes a secret. Depending on the vault implementation, this might mean a soft delete or not be even permissible.
     *
     * @param key the secret's key
     */
    Result<Void> deleteSecret(String key);

    /**
     * Get the secret from the vault.
     * <p>
     * If vault partitioning is not set up, the implementation must fall back to the default vault partition.
     *
     * @param vaultPartition The vault partition to use, for example, a participant context ID. This might be null, which
     *                       indicates that the secret is supposed to be stored in a "default partition".
     * @param key            The name of the secret. Must be unique within a vault partition.
     * @return The (serialized) secret value or null if not found.
     */
    default String resolveSecret(@Nullable String vaultPartition, String key) {
        return resolveSecret(key);
    }

    /**
     * Store a secret in the vault within the given vault partition.
     * <p>
     * If vault partitioning is not set up, the implementation must fall back to the default vault partition.
     *
     * @param vaultPartition The vault partition to use, for example, a participant context ID. This might be null, which
     *                       indicates that the secret is supposed to be stored in a "default partition".
     * @param key            The name of the secret. Must be unique within a vault partition.
     * @param value          The (serialized) secret value.
     * @return A {@link Result} indicating success or failure.
     */
    default Result<Void> storeSecret(@Nullable String vaultPartition, String key, String value) {
        return storeSecret(key, value);
    }

    /**
     * Delete a secret from the vault from the given vault partition. Note that in some vaults, deletion is a long-running asynchronous operation.
     * This method call must return immediately and not wait for the async operation.
     * <p>
     * If vault partitioning is not set up, the implementation must fall back to the default vault partition.
     *
     * @param vaultPartition The vault partition to use, for example, a participant context ID. This might be null, which
     *                       indicates that the secret is supposed to be stored in a "default partition".
     * @param key            The name of the secret. Must be unique within a vault partition.
     * @return A {@link Result} indicating success or failure.
     */
    default Result<Void> deleteSecret(@Nullable String vaultPartition, String key) {
        return deleteSecret(key);
    }
}
