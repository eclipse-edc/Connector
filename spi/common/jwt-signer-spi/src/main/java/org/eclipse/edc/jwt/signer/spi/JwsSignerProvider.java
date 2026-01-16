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

package org.eclipse.edc.jwt.signer.spi;

import com.nimbusds.jose.JWSSigner;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.result.Result;

/**
 * A JwsSignerProvider provides a {@link JWSSigner} for a given private key ID.
 */
@ExtensionPoint
public interface JwsSignerProvider {
    /**
     * Creates a {@link JWSSigner} for the given private key ID.
     *
     * @param privateKeyId The ID of the private key, used for key lookup, e.g., in a secure vault
     * @deprecated Please use {@link #createJwsSigner(String, String)} instead.
     */
    @Deprecated(since = "0.15.0")
    Result<JWSSigner> createJwsSigner(String privateKeyId);

    /**
     * Creates a {@link JWSSigner} for the given private key ID.
     *
     * @param participantContextId The ID of the participant context. Might be used to determine the lookup path or location
     * @param privateKeyId         The ID of the private key, used for key lookup, e.g., in a secure vault
     */
    Result<JWSSigner> createJwsSigner(String participantContextId, String privateKeyId);
}
