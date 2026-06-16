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

package org.eclipse.edc.vault.hashicorp.spi.auth;

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.jetbrains.annotations.Nullable;

/**
 * Creates {@link HashicorpVaultTokenProvider} instances bound to a particular vault partition.
 * <p>
 * A single connector runtime may serve multiple participant contexts, each mapped to its own vault partition. When
 * token-exchange authentication is used, the token presented to the vault must be scoped to that partition (its
 * participant context id ends up in the {@code sub} claim), so a provider must be created per partition. The
 * {@code resource} passed to {@link #create(String)} is the partition key (the participant context id), or
 * {@code null} for the default partition.
 */
@ExtensionPoint
@FunctionalInterface
public interface HashicorpVaultTokenProviderFactory {

    /**
     * Returns a token provider for the given vault partition.
     *
     * @param resource the vault partition / participant context id, or {@code null} for the default partition.
     * @return a {@link HashicorpVaultTokenProvider} for that partition.
     */
    HashicorpVaultTokenProvider create(@Nullable String resource);

}
