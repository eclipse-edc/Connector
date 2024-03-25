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

import org.eclipse.edc.keys.spi.PublicKeyResolver;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

/**
 * Interface for validating token.
 */
@FunctionalInterface
public interface TokenValidationService {
    /**
     * Validates the token and offers possibility for additional information for validations.
     *
     * @param tokenRepresentation A token representation including the token to verify.
     * @param publicKeyResolver   A {@link PublicKeyResolver} to obtain the public key with which to verify the token
     * @param rules               token validation rules that apply to the token
     * @return Result of the validation.
     */
    default Result<ClaimToken> validate(TokenRepresentation tokenRepresentation, PublicKeyResolver publicKeyResolver, TokenValidationRule... rules) {
        return validate(tokenRepresentation, publicKeyResolver, Arrays.asList(rules));
    }

    /**
     * Validates the token and offers possibility for additional information for validations.
     *
     * @param tokenRepresentation A token representation including the token to verify.
     * @param publicKeyResolver   A {@link PublicKeyResolver} to obtain the public key with which to verify the token
     * @param rules               token validation rules that apply to the token. Assume to be unmodifiable.
     * @return Result of the validation.
     */
    Result<ClaimToken> validate(TokenRepresentation tokenRepresentation, PublicKeyResolver publicKeyResolver, List<TokenValidationRule> rules);

    /**
     * Validates the token and offers possibility for additional information for validations.
     *
     * @param token             A token to verify.
     * @param publicKeyResolver A {@link PublicKeyResolver} to obtain the public key with which to verify the token
     * @param rules             token validation rules that apply to the token. Assume to be unmodifiable.
     * @return Result of the validation.
     */
    default Result<ClaimToken> validate(String token, PublicKeyResolver publicKeyResolver, List<TokenValidationRule> rules) {
        var tokenRepresentation = TokenRepresentation.Builder.newInstance()
                .token(token)
                .build();
        return validate(tokenRepresentation, publicKeyResolver, rules);
    }

    /**
     * Validates the token.
     *
     * @param token             The token to be validated.
     * @param publicKeyResolver A {@link PublicKeyResolver} to obtain the public key with which to verify the token
     * @param rules             token validation rules that apply to the token
     * @return Result of the validation.
     */
    default Result<ClaimToken> validate(@NotNull String token, PublicKeyResolver publicKeyResolver, TokenValidationRule... rules) {
        var tokenRepresentation = TokenRepresentation.Builder.newInstance()
                .token(token)
                .build();
        return validate(tokenRepresentation, publicKeyResolver, rules);
    }
}
