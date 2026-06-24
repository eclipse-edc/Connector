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

package org.eclipse.edc.iam.decentralizedclaims.spi.verification;

import org.eclipse.edc.spi.result.Result;

/**
 * Interface for verifying VerifiableCredentials and VerifiablePresentations.
 * Both can be represented as JWT or JSON-LD (linked-data-proof).
 */
public interface CredentialVerifier {
    /**
     * Determines whether the given rawInput can be handled by a particular implementation.
     *
     * @param rawInput the input to be checked
     * @return true if the rawInput can be handled, false otherwise
     */
    boolean canHandle(String rawInput);

    /**
     * Verifies the given raw input string. If the input represents a VerifiablePresentation, implementors must
     * extract all VerifiableCredentials and delegate back to the verifierContext to verify them.
     *
     * @param rawInput        the raw input to be verified
     * @param verifierContext the verifier context containing all available verifiers
     * @return a {@link Result} object indicating the result of the verification process.
     */
    Result<Void> verify(String rawInput, VerifierContext verifierContext);
}