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

package org.eclipse.edc.vault.hashicorp.auth;

import org.eclipse.edc.vault.hashicorp.spi.auth.HashicorpVaultTokenProvider;

import static java.util.Objects.requireNonNull;

/**
 * Implements the token auth method of the HashiCorp vault. Returns the configured token.
 */
public class HashicorpVaultTokenProviderImpl implements HashicorpVaultTokenProvider {

    private final String token;

    public HashicorpVaultTokenProviderImpl(String token) {
        requireNonNull(token, "Vault token must not be null");
        this.token = token;
    }

    @Override
    public String vaultToken() {
        return token;
    }
}
