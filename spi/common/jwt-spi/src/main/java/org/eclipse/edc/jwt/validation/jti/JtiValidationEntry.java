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

package org.eclipse.edc.jwt.validation.jti;

import org.jetbrains.annotations.Nullable;

import java.time.Instant;

/**
 * Represents one database row to track JTI entries.
 *
 * @param tokenId             The JWT Token ID (="jti")
 * @param expirationTimestamp optional timestamp to enable auto-cleanup
 */
public record JtiValidationEntry(String tokenId, @Nullable Instant expirationTimestamp) {
    public JtiValidationEntry(String tokenId) {
        this(tokenId, null);
    }

    /**
     * checks whether the token is expired or not. If no expirationTimestamp was specified, the token never expires and this method always returns {@code false}
     */
    public boolean isExpired() {
        if (expirationTimestamp == null) {
            return false;
        }

        return expirationTimestamp.isBefore(Instant.now());
    }
}
