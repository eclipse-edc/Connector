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

package org.eclipse.edc.spi.security;

import org.eclipse.edc.spi.result.Result;

/**
 * Provides secure storage for secrets. Each secret is identified by a key and must be provided in serialized form.
 * Secrets are stored within a "vault partition", which is used to isolate individual secrets from each other. Depending on the
 * implementation, individual "partitions" might require authentication and thus can be used as security boundary.
 */
public interface ParticipantVault {

    /**
     * Get the secret from the vault.s
     *
     * @param vaultPartition The vault partition to use, for example, a participant context ID.
     * @param key            The name of the secret. Must be unique within a vault partition.
     * @return The (serialized) secret value or null if not found.
     */
    String resolveSecret(String vaultPartition, String key);

    /**
     * Store a secret in the vault within the given vault partition.
     *
     * @param vaultPartition The vault partition to use, for example, a participant context ID.
     * @param key            The name of the secret. Must be unique within a vault partition.
     * @param value          The (serialized) secret value.
     * @return A {@link Result} indicating success or failure.
     */
    Result<Void> storeSecret(String vaultPartition, String key, String value);

    /**
     * Delete a secret from the vault from the given vault partition. Note that in some vaults, deletion is a long-running asynchronous operation.
     * This method call must return immediately and not wait for the async operation.
     *
     * @param vaultPartition The vault partition to use, for example, a participant context ID.
     * @param key            The name of the secret. Must be unique within a vault partition.
     * @return A {@link Result} indicating success or failure.
     */
    Result<Void> deleteSecret(String vaultPartition, String key);
}
