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

package org.eclipse.edc.token.spi;

import org.jetbrains.annotations.Nullable;

import java.security.PrivateKey;

/**
 * POJO that wraps a {@link PrivateKey} and a key identifier, that can be used downstream to obtain the corresponding
 * public key material.
 * For example, when signing JWTs, one would add a URL or a DID identifier with the key fragment ("did:web:identifier#key-1"), so
 * that verifiers can easily obtain the key.
 * <p>
 * This is optional, as not all identity services require such an ID.
 *
 * @param signingKey The private key which is used to sign a token
 * @param keyId      The (optional) identifier with which to obtain the public key material
 */
public record SignatureInfo(PrivateKey signingKey, @Nullable String keyId) {
    public SignatureInfo(PrivateKey signingKey) {
        this(signingKey, null);
    }
}
