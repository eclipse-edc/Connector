/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.iam.identitytrust.verification;

import org.eclipse.edc.identitytrust.model.VerifiablePresentationContainer;
import org.eclipse.edc.spi.result.Result;

/**
 * Verifies VerifiablePresentations, which are present in JWT format. Only the cryptographic integrity is asserted
 */
class JwtPresentationVerifier {
    /**
     * Computes the cryptographic integrity of a VerifiablePresentation when it's represented as JWT
     *
     * @param container The base64-encoded JWT string
     */
    public Result<Void> verifyPresentation(VerifiablePresentationContainer container) {
        throw new UnsupportedOperationException("not yet implemented!");
    }
}
