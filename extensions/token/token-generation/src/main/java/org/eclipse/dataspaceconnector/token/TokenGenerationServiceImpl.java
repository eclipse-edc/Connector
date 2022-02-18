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
 *
 */

package org.eclipse.dataspaceconnector.token;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.dataspaceconnector.spi.iam.TokenGenerationService;
import org.eclipse.dataspaceconnector.spi.iam.TokenRepresentation;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class TokenGenerationServiceImpl implements TokenGenerationService {

    private final JWSSigner tokenSigner;
    private final JWSAlgorithm jwsAlgorithm;

    public TokenGenerationServiceImpl(JWSSigner signer) {
        this.tokenSigner = signer;
        if (tokenSigner instanceof ECDSASigner) {
            jwsAlgorithm = JWSAlgorithm.ES256;
        } else {
            jwsAlgorithm = JWSAlgorithm.RS256;
        }
    }

    @Override
    public Result<TokenRepresentation> generate(@NotNull Map<String, Object> claims) {
        var builder = new JWTClaimsSet.Builder();
        claims.forEach(builder::claim);
        var claimsSet = builder.build();
        var header = new JWSHeader(jwsAlgorithm);
        var token = new SignedJWT(header, claimsSet);
        try {
            token.sign(tokenSigner);
        } catch (JOSEException e) {
            return Result.failure("Failed to sign token");
        }
        return Result.success(createTokenRepresentation(token.serialize(), claimsSet));
    }

    private static TokenRepresentation createTokenRepresentation(String token, JWTClaimsSet claimsSet) {
        var builder = TokenRepresentation.Builder.newInstance().token(token);
        if (claimsSet.getExpirationTime() != null) {
            builder.expiresIn(claimsSet.getExpirationTime().getTime());
        }
        return builder.build();
    }
}
