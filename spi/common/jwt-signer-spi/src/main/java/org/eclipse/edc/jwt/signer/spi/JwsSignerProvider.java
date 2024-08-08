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
@FunctionalInterface
public interface JwsSignerProvider {
    Result<JWSSigner> createJwsSigner(String privateKeyId);
}
