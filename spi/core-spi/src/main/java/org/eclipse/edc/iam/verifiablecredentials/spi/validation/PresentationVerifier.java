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

package org.eclipse.edc.iam.verifiablecredentials.spi.validation;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiablePresentation;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiablePresentationContainer;
import org.eclipse.edc.spi.result.Result;

/**
 * A presentation verifier is used to assert that a certain {@link VerifiablePresentation}
 * is cryptographically sound, i.e. all the proofs of the presentation, and all the proofs of all the VerifiableCredentials
 * can be verified.
 * <p>
 * Note that this does <strong>not</strong> perform any revocation checks, or any other business-related checks.
 */
@FunctionalInterface
public interface PresentationVerifier {
    /**
     * Verifies the cryptographic integrity of a VerifiablePresentation.
     *
     * @param container The VP in its raw String representation. This could be JSON-LD or a JWT
     * @param audience  the expected audience of the presentation
     * @return {@link Result#success()} if valid, {@link Result#failure(String)} otherwise, giving an indication of the error.
     */
    Result<Void> verifyPresentation(VerifiablePresentationContainer container, String audience);
}
