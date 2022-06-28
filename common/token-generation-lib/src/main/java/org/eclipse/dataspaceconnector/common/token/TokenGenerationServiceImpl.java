/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *       Microsoft Corporation - Simplified token representation
 *
 */

package org.eclipse.dataspaceconnector.common.token;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.iam.TokenRepresentation;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.jetbrains.annotations.NotNull;

import java.security.PrivateKey;
import java.security.interfaces.ECPrivateKey;
import java.util.Arrays;
import java.util.Objects;

public class TokenGenerationServiceImpl implements TokenGenerationService {

    private static final String KEY_ALGO_RSA = "RSA";
    private static final String KEY_ALGO_EC = "EC";

    private final JWSSigner tokenSigner;
    private final JWSAlgorithm jwsAlgorithm;

    public TokenGenerationServiceImpl(PrivateKey privateKey) {
        Objects.requireNonNull(privateKey, "Private key must not be null");
        this.tokenSigner = createSigner(privateKey);
        if (tokenSigner instanceof ECDSASigner) {
            jwsAlgorithm = JWSAlgorithm.ES256;
        } else {
            jwsAlgorithm = JWSAlgorithm.RS256;
        }
    }

    @Override
    public Result<TokenRepresentation> generate(@NotNull JwtDecorator... decorators) {
        var headerBuilder = new JWSHeader.Builder(jwsAlgorithm);
        var claimsBuilder = new JWTClaimsSet.Builder();
        Arrays.stream(decorators).forEach(decorator -> decorator.decorate(headerBuilder, claimsBuilder));
        var claims = claimsBuilder.build();

        var token = new SignedJWT(headerBuilder.build(), claims);
        try {
            token.sign(tokenSigner);
        } catch (JOSEException e) {
            return Result.failure("Failed to sign token");
        }
        return Result.success(TokenRepresentation.Builder.newInstance().token(token.serialize()).build());
    }

    /**
     * Generate a token signer based on a private key.
     */
    private static JWSSigner createSigner(PrivateKey privateKey) {
        switch (privateKey.getAlgorithm()) {
            case KEY_ALGO_EC:
                try {
                    return new ECDSASigner((ECPrivateKey) privateKey);
                } catch (JOSEException e) {
                    throw new EdcException("Failed to generate token signed for EC key: " + e.getMessage());
                }
            case KEY_ALGO_RSA:
                return new RSASSASigner(privateKey);
            default:
                throw new EdcException("Key algorithm not handled: " + privateKey.getAlgorithm());
        }
    }
}
