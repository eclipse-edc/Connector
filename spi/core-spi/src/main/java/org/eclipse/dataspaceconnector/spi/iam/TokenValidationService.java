/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.spi.iam;

import org.eclipse.dataspaceconnector.spi.result.Result;
import org.jetbrains.annotations.NotNull;

/**
 * Interface for validating token.
 */
@FunctionalInterface
public interface TokenValidationService {
    /**
     * Validates the token and offers possibility for additional information for validations.
     *
     * @param tokenRepresentation A token representation including the token to verify.
     * @return Result of the validation.
     */
    Result<ClaimToken> validate(TokenRepresentation tokenRepresentation);

    /**
     * Validates the token.
     *
     * @param token The token to be validated.
     * @return Result of the validation.
     */
    default Result<ClaimToken> validate(@NotNull String token) {
        var tokenRepresentation = TokenRepresentation.Builder.newInstance()
                .token(token)
                .build();

        return validate(tokenRepresentation);
    }
}
