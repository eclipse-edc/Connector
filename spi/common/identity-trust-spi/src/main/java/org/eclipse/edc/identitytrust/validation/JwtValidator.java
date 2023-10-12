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

package org.eclipse.edc.identitytrust.validation;

import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;

/**
 * Validates a Self-Issued ID token, that is expected to adhere to the IATP spec.
 * <p>
 * Validation refers to the structural integrity of a JWT, i.e. all claims are there and contain the proper values.
 * This does <strong>mean</strong> cryptographic integrity!
 *
 * @see org.eclipse.edc.identitytrust.verification.JwtVerifier cryptographic verification
 */
public interface JwtValidator {
    Result<ClaimToken> validateToken(TokenRepresentation token, String audience);
}
